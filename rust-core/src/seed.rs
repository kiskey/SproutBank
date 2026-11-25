use crate::model::DailyPrice;
use chrono::{Duration, NaiveDate};

fn lcg(seed: &mut u64) -> f64 {
    *seed = seed.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
    ((*seed >> 33) as f64) / (u32::MAX as f64)
}

fn base_cents(symbol: &str) -> i64 {
    match symbol {
        "VOO" => 47_500,
        "SCHG" => 25_200,
        "VT" => 11_050,
        "VTI" => 27_300,
        _ => 10_000,
    }
}

fn drift_bp_per_day(symbol: &str) -> f64 {
    match symbol {
        "VOO" => 4.5,
        "SCHG" => 5.5,
        "VT" => 3.5,
        "VTI" => 4.2,
        _ => 3.0,
    }
}

/// Deterministic pseudo-random-walk price history, ending on `end_date`.
pub fn seed_offline_prices(
    symbol: String,
    start_date: String,
    end_date: String,
) -> Vec<DailyPrice> {
    let start = NaiveDate::parse_from_str(&start_date, "%Y-%m-%d")
        .unwrap_or_else(|_| NaiveDate::from_ymd_opt(2023, 1, 1).unwrap());
    let end = NaiveDate::parse_from_str(&end_date, "%Y-%m-%d")
        .unwrap_or_else(|_| chrono::Local::now().date_naive());
    let mut rng_seed: u64 = (symbol.bytes().map(|b| b as u64).sum::<u64>())
        .wrapping_mul(31)
        .wrapping_add(start.num_days_from_ce() as u64);

    let base = base_cents(&symbol) as f64;
    let drift = drift_bp_per_day(&symbol) / 10_000.0;
    let mut price = base;
    let mut out = Vec::new();
    let mut day = start;
    while day <= end {
        // skip weekends
        let wd = day.format("%u").to_string().parse::<u32>().unwrap_or(1);
        if wd <= 5 {
            let noise = (lcg(&mut rng_seed) - 0.5) * 0.012;
            price *= 1.0 + drift + noise;
            out.push(DailyPrice {
                symbol: symbol.clone(),
                date: day.format("%Y-%m-%d").to_string(),
                close_cents: price.round() as i64,
            });
        }
        day += Duration::days(1);
    }
    out
}
