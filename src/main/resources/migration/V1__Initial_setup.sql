CREATE TABLE IF NOT EXISTS sku_handle_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT,
    file_id TEXT,
    sku_num TEXT,
    sku_config TEXT,
    update_time INTEGER
);


CREATE TABLE IF NOT EXISTS sku_detail_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id TEXT,
    no INTEGER,
    sku_context TEXT
);