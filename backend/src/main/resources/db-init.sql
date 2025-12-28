-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- Refresh Tokens Table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL
);

-- Employees Table
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY,
    member_id VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    uan_number VARCHAR(255) UNIQUE,
    ip_number VARCHAR(255) UNIQUE,
    bank_account_no VARCHAR(255) UNIQUE,
    ifsc_code VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    inactive_date DATE,
    category VARCHAR(50)
);

-- Payroll Periods Table
-- Payroll Periods Table
CREATE TABLE IF NOT EXISTS payroll_periods (
    id UUID PRIMARY KEY,
    period_month INTEGER NOT NULL,
    period_year INTEGER NOT NULL,
    last_working_day DATE,
    status VARCHAR(50) NOT NULL,
    total_wages_paid DECIMAL(19, 2),
    created_at TIMESTAMP,
    CONSTRAINT uk_period_month_year UNIQUE (period_month, period_year)
);

-- Payroll Entries Table
CREATE TABLE IF NOT EXISTS payroll_entries (
    id UUID PRIMARY KEY,
    period_id UUID NOT NULL REFERENCES payroll_periods(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    days_worked INTEGER,
    wages_earned DECIMAL(10, 2),
    advance_deduction DECIMAL(10, 2),
    epf_member_share DECIMAL(10, 2),
    epf_contractor_share DECIMAL(10, 2),
    esi_member_share DECIMAL(10, 2),
    esi_contractor_share DECIMAL(10, 2),
    bonus_share DECIMAL(10, 2),
    net_payable DECIMAL(10, 2),
    utr_number VARCHAR(255)
);

-- Payroll Entry Days (Element Collection)
CREATE TABLE IF NOT EXISTS payroll_entry_days (
    entry_id UUID NOT NULL REFERENCES payroll_entries(id),
    active_day INTEGER
);
