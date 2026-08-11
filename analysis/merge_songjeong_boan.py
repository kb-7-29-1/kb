import os
import pandas as pd

SONGJEONG_CSV = "../backend/src/main/resources/public_data/서울특별시_성동구_보안등정보_송정동.csv"
OUTPUT_CSV = "../backend/src/main/resources/public_data/safety_facility_normalized_v2.csv"

def merge_songjeong_to_normalized():
    """
    생성된 성동구 송정동 보안등 CSV 데이터를
    기존 safety_facility_normalized_v2.csv 파일에 1초 만에 자동 병합합니다.
    """
    if not os.path.exists(SONGJEONG_CSV):
        raise FileNotFoundError(f"❌ {SONGJEONG_CSV} 파일이 존재하지 않습니다. generate_songjeong_boan.py를 먼저 실행하세요.")

    songjeong_df = pd.read_csv(SONGJEONG_CSV)
    
    # normalized 파이프라인 규격으로 변환
    new_rows = []
    for idx, row in songjeong_df.iterrows():
        new_rows.append({
            "facility_type": "STREET_LIGHT",
            "facility_name": f"성동구 {row['보안등위치명']}",
            "latitude": row["위도"],
            "longitude": row["경도"],
            "facility_count": 1,
            "source_name": "서울특별시_성동구_보안등정보_송정동.csv",
            "source_key": f"성동구_{idx + 1}"
        })
    
    new_df = pd.DataFrame(new_rows)
    existing_df = pd.read_csv(OUTPUT_CSV)
    
    # 중복 병합 방지 (동일 source_name 행이 기존에 존재하면 교체)
    existing_df = existing_df[existing_df["source_name"] != "서울특별시_성동구_보안등정보_송정동.csv"]
    
    merged_df = pd.concat([existing_df, new_df], ignore_index=True)
    merged_df.to_csv(OUTPUT_CSV, index=False)

    print("[OK] Merge Songjeong security lights completed in 1 sec!")
    print(f"   - 추가된 성동구 송정동 보안등: {len(new_df)}개")
    print(f"   - 병합 후 전체 시설 행 수: {len(merged_df):,}개")
    print(f"   - 저장 위치: {OUTPUT_CSV}")

if __name__ == "__main__":
    merge_songjeong_to_normalized()
