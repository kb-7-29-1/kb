import pandas as pd

FACILITY_TYPES = ("CCTV", "STREET_LIGHT", "POLICE")


def load_facilities(csv_path):
    df = pd.read_csv(csv_path)
    df = df.dropna(subset=["latitude", "longitude"])
    df["facility_count"] = df["facility_count"].fillna(1).astype(int)

    by_type = {}
    for facility_type in FACILITY_TYPES:
        subset = df[df["facility_type"] == facility_type]
        by_type[facility_type] = subset[["latitude", "longitude", "facility_count"]].reset_index(drop=True)
    return by_type


if __name__ == "__main__":
    result = load_facilities("../backend/src/main/resources/public_data/safety_facility_normalized.csv")
    for facility_type, df in result.items():
        print(facility_type, len(df))