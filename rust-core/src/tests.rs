use crate::*;

fn pt(symbol: &str, date: &str, c: i64) -> DailyPrice {
    DailyPrice { symbol: symbol.into(), date: date.into(), close_cents: c }
}
fn ev(ts: i64, kind: LedgerKind, amt: i64) -> LedgerEvent {
    LedgerEvent { timestamp_ms: ts, kind, amount_cents: amt, note: String::new() }
}

#[test]
fn fractional_purchase_is_penny_accurate() {
    // $10 at $50 share -> 0.2 shares
    assert_eq!(buy_shares_micro(1_000, 5_000), 200_000);
}

#[test]
fn unspent_curve_grows() {
    let prices = vec![
        pt("VOO", "2024-01-01", 100),
        pt("VOO", "2024-01-02", 200),
        pt("VOO", "2024-01-03", 300),
    ];
    let ts = 1_704_067_200_000i64; // 2024-01-01
    let events = vec![ev(ts, LedgerKind::Credit, 1_000)];
    let s = build_portfolio_series(prices, events);
    assert_eq!(s.len(), 3);
    assert!(s[2].cash_value_cents > s[0].cash_value_cents);
}

#[test]
fn spend_impact_reflects_proportional_sale() {
    let prices = vec![
        pt("VOO", "2024-01-01", 100),
        pt("VOO", "2024-01-02", 100),
        pt("VOO", "2024-01-03", 200),
    ];
    let d1 = 1_704_067_200_000i64;
    let d2 = d1 + 86_400_000;
    let events = vec![
        ev(d1, LedgerKind::Credit, 100),  // buys 1 share
        ev(d2, LedgerKind::Spend, 100),   // sells 1 share
    ];
    let r = compute_spend_impact(prices, events);
    // After spending $1, shares = 0 → curve flat at 0
    assert_eq!(r.after_spend.last().unwrap().cash_value_cents, 0);
    // Unspent curve should have grown on day 3
    assert!(r.unspent.last().unwrap().cash_value_cents > 0);
    assert_eq!(r.sold_breakdown.len(), 1);
    assert!(r.opportunity_cost_cents > 0);
}

#[test]
fn goal_progress_shows_market_boost() {
    let prices = vec![
        pt("VOO", "2024-01-01", 100),
        pt("VOO", "2024-01-03", 150),
    ];
    let d1 = 1_704_067_200_000i64;
    let events = vec![ev(d1, LedgerKind::Credit, 10_000)];
    let g = compute_goal_progress(events, prices, 50_000);
    assert_eq!(g.cash_saved_cents, 10_000);
    assert!(g.simulated_growth_cents > g.cash_saved_cents);
    assert!(g.market_boost_cents > 0);
}

#[test]
fn seed_is_deterministic() {
    let a = seed_offline_prices("VOO".into(), "2023-01-01".into(), "2023-01-31".into());
    let b = seed_offline_prices("VOO".into(), "2023-01-01".into(), "2023-01-31".into());
    assert_eq!(a.len(), b.len());
    for (x, y) in a.iter().zip(b.iter()) { assert_eq!(x.close_cents, y.close_cents); }
}
