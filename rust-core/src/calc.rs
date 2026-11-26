use crate::model::*;
use chrono::{DateTime, Utc};

pub fn buy_shares_micro(amount_cents: i64, price_cents: i64) -> i64 {
    if price_cents <= 0 { return 0; }
    ((amount_cents as i128 * 1_000_000_i128) / price_cents as i128) as i64
}

fn ms_to_iso(ms: i64) -> String {
    DateTime::<Utc>::from_timestamp_millis(ms)
        .map(|d| d.format("%Y-%m-%d").to_string())
        .unwrap_or_else(|| "1970-01-01".into())
}

// The returned reference is bound to the element lifetime of `prices`,
// not to the slice borrow nor to `date` (which is only used for comparison).
fn trade_price<'a>(prices: &[&'a DailyPrice], date: &str) -> Option<&'a DailyPrice> {
    prices.iter()
        .find(|p| p.date.as_str() >= date)
        .copied()
        .or_else(|| prices.last().copied())
}

fn sorted_prices(prices: &[DailyPrice]) -> Vec<&DailyPrice> {
    let mut v: Vec<&DailyPrice> = prices.iter().collect();
    v.sort_by(|a, b| a.date.cmp(&b.date));
    v
}

fn apply_events(
    prices: &[DailyPrice],
    events: &[LedgerEvent],
    with_spends: bool,
) -> (Vec<PortfolioPoint>, Vec<SoldSlice>) {
    let sorted = sorted_prices(prices);
    let mut evs: Vec<&LedgerEvent> = events.iter().collect();
    evs.sort_by_key(|e| e.timestamp_ms);

    let mut trades: Vec<(String, i64, Option<&LedgerEvent>)> = Vec::new();
    for e in &evs {
        if e.kind.is_credit() {
            let d = ms_to_iso(e.timestamp_ms);
            if let Some(p) = trade_price(&sorted, &d) {
                let shares = buy_shares_micro(e.amount_cents, p.close_cents);
                trades.push((p.date.clone(), shares, Some(e)));
            }
        } else if with_spends && e.kind.is_spend() {
            let d = ms_to_iso(e.timestamp_ms);
            if let Some(p) = trade_price(&sorted, &d) {
                trades.push((p.date.clone(), -1, Some(e)));
            }
        }
    }
    trades.sort_by(|a, b| a.0.cmp(&b.0));

    let mut shares_micro: i64 = 0;
    let mut credited_cents: i64 = 0;
    let mut out = Vec::with_capacity(sorted.len());
    let mut sold = Vec::new();
    let mut t_idx = 0;

    for p in &sorted {
        while t_idx < trades.len() && trades[t_idx].0 == p.date {
            // Borrow the tuple instead of moving out of the Vec.
            let (_d, delta, ev) = &trades[t_idx];
            let delta = *delta;
            let ev = *ev;
            if let Some(e) = ev {
                if e.kind.is_credit() {
                    shares_micro += delta;
                    credited_cents += e.amount_cents;
                } else if e.kind.is_spend() {
                    let desired_value = e.amount_cents;
                    let mut sell_micro = buy_shares_micro(desired_value, p.close_cents);
                    if sell_micro > shares_micro { sell_micro = shares_micro; }
                    shares_micro -= sell_micro;
                    let cash = (sell_micro as i128 * p.close_cents as i128 / 1_000_000_i128) as i64;
                    sold.push(SoldSlice {
                        spend_timestamp_ms: e.timestamp_ms,
                        etf_symbol: p.symbol.clone(),
                        shares_micro: sell_micro,
                        price_cents: p.close_cents,
                        cash_cents: cash,
                    });
                }
            }
            t_idx += 1;
        }
        let value = (shares_micro as i128 * p.close_cents as i128 / 1_000_000_i128) as i64;
        out.push(PortfolioPoint {
            date: p.date.clone(),
            cash_value_cents: value,
            total_credited_cents: credited_cents,
        });
    }
    (out, sold)
}

pub fn build_portfolio_series(prices: Vec<DailyPrice>, events: Vec<LedgerEvent>) -> Vec<PortfolioPoint> {
    apply_events(&prices, &events, false).0
}

pub fn compute_spend_impact(prices: Vec<DailyPrice>, events: Vec<LedgerEvent>) -> SpendImpactResult {
    let unspent = apply_events(&prices, &events, false).0;
    let (after_spend, sold) = apply_events(&prices, &events, true);
    let opp = match (unspent.last(), after_spend.last()) {
        (Some(u), Some(s)) => u.cash_value_cents - s.cash_value_cents,
        _ => 0,
    };
    SpendImpactResult {
        unspent,
        after_spend,
        opportunity_cost_cents: opp.max(0),
        sold_breakdown: sold,
    }
}

pub fn compute_goal_progress(
    events: Vec<LedgerEvent>,
    etf_prices: Vec<DailyPrice>,
    target_cents: i64,
) -> GoalProgress {
    let cash_saved: i64 = events.iter()
        .map(|e| if e.kind.is_credit() { e.amount_cents } else { -e.amount_cents })
        .sum();
    let simulated = compute_spend_impact(etf_prices, events.clone())
        .after_spend.last().map(|p| p.cash_value_cents).unwrap_or(cash_saved);
    let cash_ratio = if target_cents > 0 { (cash_saved as f64 / target_cents as f64).clamp(0.0, 1.0) } else { 0.0 };
    let growth_ratio = if target_cents > 0 { (simulated as f64 / target_cents as f64).clamp(0.0, 1.0) } else { 0.0 };
    GoalProgress {
        cash_saved_cents: cash_saved,
        simulated_growth_cents: simulated,
        target_cents,
        cash_ratio,
        growth_ratio,
        market_boost_cents: simulated - cash_saved,
    }
}
