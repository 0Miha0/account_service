create table if not exists balance_audit (
    id bigint primary key generated always as identity unique,
    account_id bigint not null,
    balance_version bigint not null,
    authorization_balance decimal(18, 2) not null,
    actual_balance decimal(18, 2) not null,
    operation_id bigint not null,
    created_at timestamptz default current_timestamp not null,

    constraint fk_account_id foreign key (account_id)
        references account (id)
);

create index if not exists idx_balance_audit_account_id
    on balance_audit (account_id);
create index if not exists idx_balance_audit_created_at
    on balance_audit (created_at);
