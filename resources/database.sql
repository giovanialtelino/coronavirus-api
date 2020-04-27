create EXTENSION IF NOT EXISTS "uuid-ossp";

create TABLE coronavirus
(uuid_ uuid NOT NULL DEFAULT uuid_generate_v1(),
    CONSTRAINT pet_pkey_ PRIMARY KEY (uuid_),
    admin2 VARCHAR(50),
    fips INTEGER,
    province_state VARCHAR,
    country_region VARCHAR,
    last_update DATE,
    location POINT,
    recovered INTEGER,
    confirmed INTEGER,
    deaths INTEGER,
    active integer,
    tested INTEGER,
    file_date DATE
 );