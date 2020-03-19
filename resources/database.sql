create TABLE country
(id INTEGER PRIMARY KEY,
name TEXT,
abrev VARCHAR(10));

create TABLE coronavirus
(uuid INTEGER PRIMARY KEY,
 index_id INTEGER,
 country INTEGER REFERENCES country,
 province VARCHAR,
 location POINT,
 recovered INTEGER,
 confirmed INTEGER,
 deaths INTEGER,
 date DATE,
 url TEXT,
 population INTEGER
 );
