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
    static final double STREET_LIGHT_ROUTE_RADIUS_METERS = 30.0;
    static final double POLICE_ROUTE_RADIUS_METERS = 300.0;

    public SafetyRouteCandidateDTO calculate(PedestrianRoute route, List<SafetyFacilityVO> facilities) {
        if (route == null || route.getRoutePoints() == null || route.getRoutePoints().size() < 2) {
            throw new IllegalArgumentException("경로 좌표는 2개 이상이어야 합니다.");
        }

        List<ProjectedPoint> projectedRoute = projectRoute(route.getRoutePoints());
        List<ProjectedFacility> cctvs = projectFacilities(filterByType(facilities, "CCTV"), projectedRoute.get(0));
        List<ProjectedFacility> streetLights = projectFacilities(
                filterByType(facilities, "STREET_LIGHT"),
                projectedRoute.get(0)
        );
        List<ProjectedFacility> policeStations = projectFacilities(
                filterByType(facilities, "POLICE"),
                projectedRoute.get(0)
        );

        int cctvCount = countFacilitiesNearRoute(projectedRoute, cctvs, CCTV_ROUTE_RADIUS_METERS);
        int streetLightCount = countFacilitiesNearRoute(
                projectedRoute,
                streetLights,
                STREET_LIGHT_ROUTE_RADIUS_METERS
        );
        double cctvCoverage = calculateCoverage(
                projectedRoute,
                cctvs,
                50.0,
                CCTV_ROUTE_RADIUS_METERS
        );
        double streetLightCoverage = calculateCoverage(
                projectedRoute,
                streetLights,
                30.0,
                STREET_LIGHT_ROUTE_RADIUS_METERS
        );
        boolean hasPoliceStation = hasFacilityNearRoute(
                projectedRoute,
                policeStations,
                POLICE_ROUTE_RADIUS_METERS
        );

        double routeDistanceMeters = route.getDistanceMeters() != null && route.getDistanceMeters() > 0
                ? route.getDistanceMeters()
                : polylineLength(projectedRoute);
        double averageGapMeters = cctvCount == 0
                ? Double.POSITIVE_INFINITY
                : routeDistanceMeters / cctvCount;

        int cctvDensityPenalty = calculateCctvDensityPenalty(averageGapMeters);
        int cctvCoveragePenalty = calculateCctvCoveragePenalty(cctvCoverage);
        int streetLightPenalty = calculateStreetLightCoveragePenalty(streetLightCoverage);
        int policePenalty = hasPoliceStation ? 0 : 20;
        int totalPenalty = cctvDensityPenalty
                + cctvCoveragePenalty
                + streetLightPenalty
                + policePenalty;
        int safetyScore = Math.max(
                0,
                (int) Math.round(100 - totalPenalty / 2.0)
        );

        SafetyScoreBreakdownDTO breakdown = new SafetyScoreBreakdownDTO();
        breakdown.setCctvDensityPenalty(cctvDensityPenalty);
        breakdown.setCctvCoveragePenalty(cctvCoveragePenalty);
        breakdown.setStreetLightCoveragePenalty(streetLightPenalty);
        breakdown.setPoliceStationPenalty(policePenalty);
        breakdown.setTotalPenalty(totalPenalty);
        breakdown.setCctvCount(cctvCount);
        breakdown.setStreetLightCount(streetLightCount);
        breakdown.setHasPoliceStation(hasPoliceStation);
        breakdown.setCctvAverageGapMeters(
                Double.isFinite(averageGapMeters) ? round1(averageGapMeters) : null
        );
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

    int calculateCctvDensityPenalty(double averageGapMeters) {
        if (averageGapMeters <= 50.0) return 0;
        if (averageGapMeters <= 75.0) return 5;
        if (averageGapMeters <= 100.0) return 10;
        if (averageGapMeters <= 150.0) return 15;
        return 20;
    }

    int calculateCctvCoveragePenalty(double coveragePercent) {
        if (coveragePercent >= 80.0) return 0;
        if (coveragePercent >= 60.0) return 4;
        if (coveragePercent >= 40.0) return 8;
        if (coveragePercent >= 20.0) return 12;
        return 15;
    }

    int calculateStreetLightCoveragePenalty(double coveragePercent) {
        if (coveragePercent >= 80.0) return 0;
        if (coveragePercent >= 60.0) return 10;
        if (coveragePercent >= 40.0) return 20;
        if (coveragePercent >= 20.0) return 30;
        return 45;
    }

    private int countFacilitiesNearRoute(
            List<ProjectedPoint> route,
            List<ProjectedFacility> facilities,
            double radiusMeters
    ) {
        int total = 0;
        for (ProjectedFacility facility : facilities) {
            if (distancePointToPolyline(facility.point(), route) <= radiusMeters) {
                total += Math.max(1, facility.count());
            }
        }
        return total;
    }

    /**
     * 경로를 sectionLengthMeters 간격의 구간으로 나눈 뒤 각 구간 중점이 시설 반경 안인지 평가합니다.
     */
    private double calculateCoverage(
            List<ProjectedPoint> route,
            List<ProjectedFacility> facilities,
            double sectionLengthMeters,
            double radiusMeters
    ) {
        double totalLength = polylineLength(route);
        if (totalLength <= 0.0 || facilities.isEmpty()) {
            return 0.0;
        }

        int sectionCount = Math.max(1, (int) Math.ceil(totalLength / sectionLengthMeters));
        int coveredCount = 0;
        for (int index = 0; index < sectionCount; index++) {
            double targetDistance = Math.min(
                    totalLength,
                    (index + 0.5) * sectionLengthMeters
            );
            ProjectedPoint sample = pointAtDistance(route, targetDistance);
            if (isNearAnyFacility(sample, facilities, radiusMeters)) {
                coveredCount++;
            }
        }
        return coveredCount * 100.0 / sectionCount;
    }

    private boolean isNearAnyFacility(
            ProjectedPoint point,
            List<ProjectedFacility> facilities,
            double radiusMeters
    ) {
        double radiusSquared = radiusMeters * radiusMeters;
        for (ProjectedFacility facility : facilities) {
            double dx = point.x() - facility.point().x();
            double dy = point.y() - facility.point().y();
            if (dx * dx + dy * dy <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFacilityNearRoute(
            List<ProjectedPoint> route,
            List<ProjectedFacility> facilities,
            double radiusMeters
    ) {
        for (ProjectedFacility facility : facilities) {
            if (distancePointToPolyline(facility.point(), route) <= radiusMeters) {
                return true;
            }
        }
        return false;
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
                    origin.getLongitude()
            ));
        }
        return projected;
    }

    private List<ProjectedFacility> projectFacilities(
            List<SafetyFacilityVO> facilities,
            ProjectedPoint routeOrigin
    ) {
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
                            routeOrigin.originLongitude()
                    ),
                    facility.getFacilityCount() == null ? 1 : facility.getFacilityCount()
            ));
        }
        return projected;
    }

    private ProjectedPoint project(
            double latitude,
            double longitude,
            double originLatitude,
            double originLongitude
    ) {
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
                        start.originLongitude()
                );
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
                    distancePointToSegment(point, polyline.get(i - 1), polyline.get(i))
            );
        }
        return minimum;
    }

    private double distancePointToSegment(
            ProjectedPoint point,
            ProjectedPoint start,
            ProjectedPoint end
    ) {
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
        if (safetyScore >= 80) return "SAFE";
        if (safetyScore >= 60) return "WARNING";
        return "DANGER";
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record ProjectedPoint(
            double x,
            double y,
            double originLatitude,
            double originLongitude
    ) {
    }

    private record ProjectedFacility(ProjectedPoint point, int count) {
    }
}
