ALTER TABLE weather_extremes_daily
ALTER COLUMN severity TYPE DOUBLE PRECISION
USING severity::double precision;
