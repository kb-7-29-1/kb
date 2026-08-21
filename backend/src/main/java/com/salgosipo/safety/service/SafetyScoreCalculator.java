package com.salgosipo.safety.service;

import com.salgosipo.safety.domain.PedestrianRoute;
import com.salgosipo.safety.domain.SafetyFacilityVO;
import com.salgosipo.safety.dto.RoutePointDTO;
import com.salgosipo.safety.dto.SafetyRouteCandidateDTO;
import com.salgosipo.safety.dto.SafetyScoreBreakdownDTO;
import java.util.ArrayList;
import java.util.List;

public class SafetyScoreCalculator {

    static final double CCTV_ROUTE_RADIUS_METERS = 50.0;
    static final double STREET_LIGHT_ROUTE_RADIUS_METERS = 20.0;
    static final double POLICE_ROUTE_RADIUS_METERS = 500.0;

    public SafetyRouteCandidateDTO calculate(PedestrianRoute route, List<SafetyFacilityVO> facilities) {
        if (route == null || route.getRoutePoints() == null || route.getRoutePoints().size() < 2) {
            throw new IllegalArgumentException("경로 좌표는 2개 이상이어야 합니다.");
        }

        List<ProjectedPoint> projectedRoute = projectRoute(route.getRoutePoints());
        List<ProjectedFacility> cctvs = projectFacilities(filterByType(facilities, "CCTV"), projectedRoute.get(0));
        List<ProjectedFacility> streetLights = projectFacilities(
                filterByType(facilities, "STREET_LIGHT"),
                projectedRoute.get(0));
        List<ProjectedFacility> policeStations = projectFacilities(
                filterByType(facilities, "POLICE"),
                projectedRoute.get(0));

        int cctvCount = countFacilitiesNearRoute(projectedRoute, cctvs, CCTV_ROUTE_RADIUS_METERS);
        int streetLightCount = countFacilitiesNearRoute(
                projectedRoute,
                streetLights,
                STREET_LIGHT_ROUTE_RADIUS_METERS);
        double cctvCoverage = calculateCoverage(
                projectedRoute,
                cctvs,
                CCTV_ROUTE_RADIUS_METERS);
        double streetLightCoverage = calculateCoverage(
                projectedRoute,
                streetLights,
                STREET_LIGHT_ROUTE_RADIUS_METERS);
        boolean hasPoliceStation = hasFacilityNearRoute(
                projectedRoute,
                policeStations,
                POLICE_ROUTE_RADIUS_METERS);
        double nearestPoliceDistanceMeters = nearestFacilityDistanceToRoute(
                projectedRoute,
                policeStations);

        double routeDistanceMeters = route.getDistanceMeters() != null && route.getDistanceMeters() > 0
                ? route.getDistanceMeters()
                : polylineLength(projectedRoute);
        double averageGapMeters = cctvCount == 0
                ? Double.POSITIVE_INFINITY
                : routeDistanceMeters / cctvCount;
        double streetLightAverageGapMeters = streetLightCount == 0
                ? Double.POSITIVE_INFINITY
                : routeDistanceMeters / streetLightCount;

        int cctvDensityPenalty = calculateCctvDensityPenalty(averageGapMeters);
        int cctvCoveragePenalty = calculateCctvCoveragePenalty(cctvCoverage);
        int streetLightPenalty = calculateStreetLightCoveragePenalty(streetLightCoverage);
        int policePenalty = hasPoliceStation ? 0 : 10;
        int totalPenalty = cctvDensityPenalty
                + cctvCoveragePenalty
                + streetLightPenalty
                + policePenalty;
        int safetyScore = Math.max(
                0,
                (int) Math.round(100 - totalPenalty / 3.0));

        SafetyScoreBreakdownDTO breakdown = new SafetyScoreBreakdownDTO();
        breakdown.setCctvDensityPenalty(cctvDensityPenalty);
        breakdown.setCctvCoveragePenalty(cctvCoveragePenalty);
        breakdown.setStreetLightCoveragePenalty(streetLightPenalty);
        breakdown.setPoliceStationPenalty(policePenalty);
        breakdown.setTotalPenalty(totalPenalty);
        breakdown.setCctvCount(cctvCount);
        breakdown.setStreetLightCount(streetLightCount);
        breakdown.setHasPoliceStation(hasPoliceStation);
        breakdown.setNearestPoliceDistanceMeters(
                Double.isFinite(nearestPoliceDistanceMeters) ? round1(nearestPoliceDistanceMeters) : null);
        breakdown.setCctvAverageGapMeters(
                Double.isFinite(averageGapMeters) ? round1(averageGapMeters) : null);
        breakdown.setStreetLightAverageGapMeters(
                Double.isFinite(streetLightAverageGapMeters) ? round1(streetLightAverageGapMeters) : null);
        breakdown.setCctvCoveragePercent(round1(cctvCoverage));
        breakdown.setStreetLightCoveragePercent(round1(streetLightCoverage));

        SafetyRouteCandidateDTO candidate = new SafetyRouteCandidateDTO();
        candidate.setRouteId(route.getRouteId());
        candidate.setSearchOption(route.getSearchOption());
        candidate.setRouteType(route.getRouteType());
        candidate.setSelected(false);
        candidate.setSafetyScore(safetyScore);
        candidate.setSafetyGrade(toGrade(safetyScore));
        candidate.setDistanceMeters(route.getDistanceMeters());
        candidate.setTotalTimeSeconds(route.getTotalTimeSeconds());
        candidate.setBreakdown(breakdown);
        candidate.setRoutePoints(route.getRoutePoints());
        return candidate;
    }

    /**
     * 점수 계산(countFacilitiesNearRoute)과 동일한 반경/거리 기준으로,
     * 실제로 경로에 영향을 준 시설물만 걸러서 반환합니다. ("경로 자세히 보기" 지도 표시용)
     */
    public List<SafetyFacilityVO> filterFacilitiesNearRoute(
            PedestrianRoute route,
            List<SafetyFacilityVO> facilities) {
        if (route == null || route.getRoutePoints() == null || route.getRoutePoints().size() < 2
                || facilities == null || facilities.isEmpty()) {
            return List.of();
        }

        List<ProjectedPoint> projectedRoute = projectRoute(route.getRoutePoints());
        ProjectedPoint origin = projectedRoute.get(0);

        List<SafetyFacilityVO> result = new ArrayList<>();
        for (SafetyFacilityVO facility : facilities) {
            if (facility.getLatitude() == null || facility.getLongitude() == null) {
                continue;
            }
            double radiusMeters = radiusForType(facility.getFacilityType());
            if (radiusMeters <= 0.0) {
                continue;
            }
            ProjectedPoint projected = project(
                    facility.getLatitude(),
                    facility.getLongitude(),
                    origin.originLatitude(),
                    origin.originLongitude());
            if (distancePointToPolyline(projected, projectedRoute) <= radiusMeters) {
                result.add(facility);
            }
        }
        return result;
    }

    private double radiusForType(String facilityType) {
        if ("CCTV".equalsIgnoreCase(facilityType))
            return CCTV_ROUTE_RADIUS_METERS;
        if ("STREET_LIGHT".equalsIgnoreCase(facilityType))
            return STREET_LIGHT_ROUTE_RADIUS_METERS;
        if ("POLICE".equalsIgnoreCase(facilityType))
            return POLICE_ROUTE_RADIUS_METERS;
        return 0.0;
    }

    int calculateCctvDensityPenalty(double averageGapMeters) {
        // CCTV 평균 간격이 50m 이하면 0점, 150m 이상이면 최대 20점 감점.
        // 그 사이는 평균 간격에 비례해 연속적으로 증가한 뒤 정수로 반올림합니다.
        double rawPenalty = (averageGapMeters - 50.0) / (150.0 - 50.0) * 20.0;
        return clampAndRoundPenalty(rawPenalty, 20);
    }

    int calculateCctvCoveragePenalty(double coveragePercent) {
        // CCTV 커버리지가 80% 이상이면 0점, 0%이면 최대 15점 감점.
        // 기존 구간별 if 감점 대신 커버리지 부족분에 비례해 연속적으로 계산합니다.
        double rawPenalty = (80.0 - coveragePercent) / 80.0 * 15.0;
        return clampAndRoundPenalty(rawPenalty, 15);
    }

    int calculateStreetLightCoveragePenalty(double coveragePercent) {
        // 가로등 커버리지가 80% 이상이면 0점, 0%이면 최대 55점 감점.
        // 기존 구간별 if 감점 대신 커버리지 부족분에 비례해 연속적으로 계산합니다.
        double rawPenalty = (80.0 - coveragePercent) / 80.0 * 55.0;
        return clampAndRoundPenalty(rawPenalty, 55);
    }

    private int clampAndRoundPenalty(double rawPenalty, int maxPenalty) {
        double clampedPenalty = Math.max(0.0, Math.min(maxPenalty, rawPenalty));
        return (int) Math.round(clampedPenalty);
    }

    private int countFacilitiesNearRoute(
            List<ProjectedPoint> route,
            List<ProjectedFacility> facilities,
            double radiusMeters) {
        int total = 0;
        for (ProjectedFacility facility : facilities) {
            if (distancePointToPolyline(facility.point(), route) <= radiusMeters) {
                total += Math.max(1, facility.count());
            }
        }
        return total;
    }

    /**
     * TMAP/Valhalla가 반환한 실제 보행 경로 polyline 전체를 연속적인 선분으로 보고
     * 시설 반경 안에 포함되는 경로 길이를 정확히 계산합니다.
     *
     * 기존처럼 30m/50m 구간의 중점만 샘플링하지 않습니다. 각 경로 선분과
     * 시설 반경 원의 교차 구간을 구한 뒤, 같은 선분에서 서로 겹치는 구간은
     * 합쳐서 한 번만 계산합니다.
     *
     * coverage = (시설 반경 안에 실제로 포함된 경로 길이 / 전체 경로 길이) * 100
     */
    private double calculateCoverage(
            List<ProjectedPoint> route,
            List<ProjectedFacility> facilities,
            double radiusMeters) {
        double totalLength = polylineLength(route);
        if (totalLength <= 0.0 || facilities.isEmpty()) {
            return 0.0;
        }

        double coveredLength = 0.0;

        for (int segmentIndex = 1; segmentIndex < route.size(); segmentIndex++) {
            ProjectedPoint start = route.get(segmentIndex - 1);
            ProjectedPoint end = route.get(segmentIndex);
            double segmentLength = distance(start, end);
            if (segmentLength <= 0.0) {
                continue;
            }

            List<CoverageInterval> intervals = new ArrayList<>();
            for (ProjectedFacility facility : facilities) {
                CoverageInterval interval = coverageIntervalOnSegment(
                        start,
                        end,
                        facility.point(),
                        radiusMeters);
                if (interval != null && interval.end() > interval.start()) {
                    intervals.add(interval);
                }
            }

            if (intervals.isEmpty()) {
                continue;
            }

            intervals.sort((left, right) -> Double.compare(left.start(), right.start()));

            double mergedStart = intervals.get(0).start();
            double mergedEnd = intervals.get(0).end();
            double coveredFraction = 0.0;

            for (int intervalIndex = 1; intervalIndex < intervals.size(); intervalIndex++) {
                CoverageInterval current = intervals.get(intervalIndex);
                if (current.start() <= mergedEnd) {
                    mergedEnd = Math.max(mergedEnd, current.end());
                } else {
                    coveredFraction += mergedEnd - mergedStart;
                    mergedStart = current.start();
                    mergedEnd = current.end();
                }
            }
            coveredFraction += mergedEnd - mergedStart;

            coveredLength += segmentLength * coveredFraction;
        }

        return Math.max(0.0, Math.min(100.0, coveredLength * 100.0 / totalLength));
    }

    /**
     * 하나의 경로 선분 P(t) = start + t(end-start), 0 <= t <= 1 과
     * 시설 중심을 기준으로 한 반경 원의 교차 구간 [tStart, tEnd]를 구합니다.
     */
    private CoverageInterval coverageIntervalOnSegment(
            ProjectedPoint start,
            ProjectedPoint end,
            ProjectedPoint facility,
            double radiusMeters) {
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        double fx = start.x() - facility.x();
        double fy = start.y() - facility.y();

        double a = dx * dx + dy * dy;
        if (a <= 0.0) {
            return null;
        }

        double b = 2.0 * (fx * dx + fy * dy);
        double c = fx * fx + fy * fy - radiusMeters * radiusMeters;
        double discriminant = b * b - 4.0 * a * c;

        if (discriminant < 0.0) {
            return null;
        }

        double sqrtDiscriminant = Math.sqrt(Math.max(0.0, discriminant));
        double t1 = (-b - sqrtDiscriminant) / (2.0 * a);
        double t2 = (-b + sqrtDiscriminant) / (2.0 * a);

        double intervalStart = Math.max(0.0, Math.min(t1, t2));
        double intervalEnd = Math.min(1.0, Math.max(t1, t2));

        if (intervalEnd <= intervalStart) {
            return null;
        }

        return new CoverageInterval(intervalStart, intervalEnd);
    }

    private boolean hasFacilityNearRoute(
            List<ProjectedPoint> route,
            List<ProjectedFacility> facilities,
            double radiusMeters) {
        for (ProjectedFacility facility : facilities) {
            if (distancePointToPolyline(facility.point(), route) <= radiusMeters) {
                return true;
            }
        }
        return false;
    }

    private double nearestFacilityDistanceToRoute(
            List<ProjectedPoint> route,
            List<ProjectedFacility> facilities) {
        return facilities.stream()
                .mapToDouble(facility -> distancePointToPolyline(facility.point(), route))
                .min()
                .orElse(Double.NaN);
    }

    private List<SafetyFacilityVO> filterByType(List<SafetyFacilityVO> facilities, String type) {
        if (facilities == null || facilities.isEmpty()) {
            return List.of();
        }
        return facilities.stream()
                .filter(facility -> type.equalsIgnoreCase(facility.getFacilityType()))
                .toList();
    }

    private List<ProjectedPoint> projectRoute(List<RoutePointDTO> routePoints) {
        RoutePointDTO origin = routePoints.get(0);
        List<ProjectedPoint> projected = new ArrayList<>(routePoints.size());
        for (RoutePointDTO point : routePoints) {
            projected.add(project(
                    point.getLatitude(),
                    point.getLongitude(),
                    origin.getLatitude(),
                    origin.getLongitude()));
        }
        return projected;
    }

    private List<ProjectedFacility> projectFacilities(
            List<SafetyFacilityVO> facilities,
            ProjectedPoint routeOrigin) {
        List<ProjectedFacility> projected = new ArrayList<>(facilities.size());
        for (SafetyFacilityVO facility : facilities) {
            if (facility.getLatitude() == null || facility.getLongitude() == null) {
                continue;
            }
            projected.add(new ProjectedFacility(
                    project(
                            facility.getLatitude(),
                            facility.getLongitude(),
                            routeOrigin.originLatitude(),
                            routeOrigin.originLongitude()),
                    facility.getFacilityCount() == null ? 1 : facility.getFacilityCount()));
        }
        return projected;
    }

    private ProjectedPoint project(
            double latitude,
            double longitude,
            double originLatitude,
            double originLongitude) {
        double earthRadius = 6_371_000.0;
        double x = Math.toRadians(longitude - originLongitude)
                * earthRadius
                * Math.cos(Math.toRadians((latitude + originLatitude) / 2.0));
        double y = Math.toRadians(latitude - originLatitude) * earthRadius;
        return new ProjectedPoint(x, y, originLatitude, originLongitude);
    }

    private double polylineLength(List<ProjectedPoint> route) {
        double total = 0.0;
        for (int i = 1; i < route.size(); i++) {
            total += distance(route.get(i - 1), route.get(i));
        }
        return total;
    }

    private ProjectedPoint pointAtDistance(List<ProjectedPoint> route, double targetDistance) {
        if (targetDistance <= 0.0) {
            return route.get(0);
        }

        double traversed = 0.0;
        for (int i = 1; i < route.size(); i++) {
            ProjectedPoint start = route.get(i - 1);
            ProjectedPoint end = route.get(i);
            double segmentLength = distance(start, end);
            if (segmentLength == 0.0) {
                continue;
            }
            if (traversed + segmentLength >= targetDistance) {
                double ratio = (targetDistance - traversed) / segmentLength;
                return new ProjectedPoint(
                        start.x() + (end.x() - start.x()) * ratio,
                        start.y() + (end.y() - start.y()) * ratio,
                        start.originLatitude(),
                        start.originLongitude());
            }
            traversed += segmentLength;
        }
        return route.get(route.size() - 1);
    }

    private double distancePointToPolyline(ProjectedPoint point, List<ProjectedPoint> polyline) {
        double minimum = Double.POSITIVE_INFINITY;
        for (int i = 1; i < polyline.size(); i++) {
            minimum = Math.min(
                    minimum,
                    distancePointToSegment(point, polyline.get(i - 1), polyline.get(i)));
        }
        return minimum;
    }

    private double distancePointToSegment(
            ProjectedPoint point,
            ProjectedPoint start,
            ProjectedPoint end) {
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        double denominator = dx * dx + dy * dy;
        if (denominator == 0.0) {
            return distance(point, start);
        }

        double t = ((point.x() - start.x()) * dx + (point.y() - start.y()) * dy) / denominator;
        t = Math.max(0.0, Math.min(1.0, t));
        double closestX = start.x() + t * dx;
        double closestY = start.y() + t * dy;
        return Math.hypot(point.x() - closestX, point.y() - closestY);
    }

    private double distance(ProjectedPoint a, ProjectedPoint b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    private String toGrade(int safetyScore) {
        if (safetyScore >= 80)
            return "SAFE";
        if (safetyScore >= 60)
            return "WARNING";
        return "DANGER";
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record CoverageInterval(double start, double end) {
    }

    private record ProjectedPoint(
            double x,
            double y,
            double originLatitude,
            double originLongitude) {
    }

    private record ProjectedFacility(ProjectedPoint point, int count) {
    }
}