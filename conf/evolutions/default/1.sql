# --- !Ups

CREATE TYPE repeat AS ENUM ('Never', 'Daily', 'Weekly', 'Monthly', 'EveryHour');

# --- !Downs

DROP TYPE IF EXISTS repeat;
