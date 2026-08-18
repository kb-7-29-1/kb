import csv, os, zipfile
from collections import defaultdict

src_dir = r'C:\Users\78831\map_data\gtfs_capital'
out_zip = r'C:\Users\78831\map_data\gtfs_motis.zip'

print('[1/6] Filtering valid Seoul stops...')
valid_stops = set()
filtered_stops = []
with open(os.path.join(src_dir, 'stops.txt'), 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        try:
            lat = float(row['stop_lat'])
            lon = float(row['stop_lon'])
            stop_id = row['stop_id'].strip()
            if 37.40 <= lat <= 37.72 and 126.73 <= lon <= 127.27 and stop_id:
                valid_stops.add(stop_id)
                filtered_stops.append({'stop_id': stop_id, 'stop_name': row['stop_name'].strip(), 'stop_lat': f'{lat:.6f}', 'stop_lon': f'{lon:.6f}'})
        except:
            pass

print(f' -> Valid Stops: {len(valid_stops):,}')

print('[2/6] Filtering pure subway & bus routes (removing airplane AR_, ferry FR_)...')
valid_routes = set()
filtered_routes = []
with open(os.path.join(src_dir, 'routes.txt'), 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        rid = row['route_id'].strip()
        rtype = row.get('route_type', '3').strip()
        if not rid.startswith('AR_') and not rid.startswith('FR_'):
            if rtype not in ['0', '1', '2', '3']:
                rtype = '3'
            valid_routes.add(rid)
            filtered_routes.append({
                'route_id': rid,
                'agency_id': 'A1',
                'route_short_name': row.get('route_short_name', '').strip() or rid,
                'route_long_name': row.get('route_long_name', '').strip() or '',
                'route_type': rtype
            })

print(f' -> Valid Routes: {len(valid_routes):,}')

print('[3/6] Filtering valid trips...')
valid_trips = set()
filtered_trips = []
with open(os.path.join(src_dir, 'trips.txt'), 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        tid = row['trip_id'].strip()
        rid = row['route_id'].strip()
        if rid in valid_routes and not tid.startswith('AR_') and not tid.startswith('FR_'):
            valid_trips.add(tid)
            filtered_trips.append({'route_id': rid, 'service_id': 'B1', 'trip_id': tid})

print(f' -> Valid Trips: {len(valid_trips):,}')

print('[4/6] Cleaning & normalizing stop_times (sequence strictly 1..N)...')
trip_stop_times = defaultdict(list)
with open(os.path.join(src_dir, 'stop_times.txt'), 'r', encoding='utf-8') as f:
    for line in f:
        parts = line.strip().split(',')
        if len(parts) >= 5:
            tid = parts[0].strip()
            arr = parts[1].strip()
            dep = parts[2].strip()
            sid = parts[3].strip()
            seq = parts[4].strip()
            if tid in valid_trips and sid in valid_stops:
                try:
                    trip_stop_times[tid].append((int(seq), arr, dep, sid))
                except:
                    pass

final_stop_times_lines = ['trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type\n']
final_valid_trips = set()
for tid, stops in trip_stop_times.items():
    if len(stops) >= 2:
        stops.sort(key=lambda x: x[0])
        final_valid_trips.add(tid)
        for idx, (_, arr, dep, sid) in enumerate(stops, start=1):
            final_stop_times_lines.append(tid + ',' + arr + ',' + dep + ',' + sid + ',' + str(idx) + ',0,0\n')

filtered_trips = [t for t in filtered_trips if t['trip_id'] in final_valid_trips]
final_used_routes = {t['route_id'] for t in filtered_trips}
filtered_routes = [r for r in filtered_routes if r['route_id'] in final_used_routes]
final_used_stops = {parts.split(',')[3] for parts in final_stop_times_lines[1:]}
filtered_stops = [s for s in filtered_stops if s['stop_id'] in final_used_stops]

print(f' -> Cleaned Records: Stops={len(filtered_stops):,}, Routes={len(filtered_routes):,}, Trips={len(filtered_trips):,}, StopTimes={len(final_stop_times_lines)-1:,}')

print('[5/6] Writing 100% MOTIS-compliant gtfs.zip...')
with zipfile.ZipFile(out_zip, 'w', zipfile.ZIP_DEFLATED) as z:
    z.writestr('agency.txt', 'agency_id,agency_name,agency_url,agency_timezone,agency_lang\nA1,KTDB,http://www.ktdb.go.kr/,Asia/Seoul,ko\n')
    z.writestr('calendar.txt', 'service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\nB1,1,1,1,1,1,1,1,20260810,20260816\n')
    z.writestr('feed_info.txt', 'feed_publisher_name,feed_publisher_url,feed_lang,feed_start_date,feed_end_date\nKTDB,http://www.ktdb.go.kr/,ko,20260810,20260816\n')
    
    stops_text = 'stop_id,stop_name,stop_lat,stop_lon\n' + '\n'.join([s['stop_id'] + ',' + s['stop_name'] + ',' + s['stop_lat'] + ',' + s['stop_lon'] for s in filtered_stops]) + '\n'
    z.writestr('stops.txt', stops_text)
    
    routes_text = 'route_id,agency_id,route_short_name,route_long_name,route_type\n' + '\n'.join([r['route_id'] + ',' + r['agency_id'] + ',' + r['route_short_name'] + ',' + r['route_long_name'] + ',' + r['route_type'] for r in filtered_routes]) + '\n'
    z.writestr('routes.txt', routes_text)
    
    trips_text = 'route_id,service_id,trip_id\n' + '\n'.join([t['route_id'] + ',' + t['service_id'] + ',' + t['trip_id'] for t in filtered_trips]) + '\n'
    z.writestr('trips.txt', trips_text)
    
    z.writestr('stop_times.txt', ''.join(final_stop_times_lines))

final_gtfs = r'C:\Users\78831\map_data\gtfs.zip'
os.replace(out_zip, final_gtfs)
print(f'[6/6] Complete! Cleaned gtfs.zip size: {os.path.getsize(final_gtfs)/(1024*1024):.1f} MB')
