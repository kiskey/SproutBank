use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum LedgerKind { Credit, Bonus, Spend, Redeem }

impl LedgerKind {
    pub fn is_credit(self) -> bool { matches!(self, Self::Credit | Self::Bonus) }
    pub fn is_spend(self) -> bool { matches!(self, Self::Spend | Self::Redeem) }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DailyPrice {
    pub symbol: String,
    pub date: String,
    pub close_cents: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LedgerEvent {
    pub timestamp_ms: i64,
    pub kind: LedgerKind,
    pub amount_cents: i64,
    pub note: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PortfolioPoint {
    pub date: String,
    pub cash_value_cents: i64,
    pub total_credited_cents: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SoldSlice {
    pub spend_timestamp_ms: i64,
    pub etf_symbol: String,
    pub shares_micro: i64,
    pub price_cents: i64,
    pub cash_cents: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SpendImpactResult {
    pub unspent: Vec<PortfolioPoint>,
    pub after_spend: Vec<PortfolioPoint>,
    pub opportunity_cost_cents: i64,
    pub sold_breakdown: Vec<SoldSlice>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GoalProgress {
    pub cash_saved_cents: i64,
    pub simulated_growth_cents: i64,
    pub target_cents: i64,
    pub cash_ratio: f64,
    pub growth_ratio: f64,
    pub market_boost_cents: i64,
}
