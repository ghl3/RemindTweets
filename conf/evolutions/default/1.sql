# --- !Ups

create table "reminders" ("id" SERIAL NOT NULL PRIMARY KEY,"user_id" BIGINT NOT NULL,"twitter_id" BIGINT NOT NULL,"createdat" timestamptz NOT NULL,"repeat" repeat NOT NULL,"firsttime" timestamptz NOT NULL,"what" VARCHAR(254) NOT NULL,"tweet_id" BIGINT NOT NULL);
create table "scheduledreminders" ("id" SERIAL NOT NULL PRIMARY KEY,"reminder_id" BIGINT NOT NULL,"user_id" BIGINT NOT NULL,"time" timestamptz NOT NULL,"executed" BOOLEAN NOT NULL,"cancelled" BOOLEAN NOT NULL,"in_progress" BOOLEAN NOT NULL,"failed" BOOLEAN NOT NULL);
create table "tweets" ("id" SERIAL NOT NULL PRIMARY KEY,"user_id" BIGINT NOT NULL,"twitter_id" BIGINT NOT NULL,"screen_name" VARCHAR(254) NOT NULL,"content" json NOT NULL,"fetchedat" timestamptz NOT NULL);
create table "users" ("id" SERIAL NOT NULL PRIMARY KEY,"screen_name" VARCHAR(254) NOT NULL,"createdat" timestamp NOT NULL);

# --- !Downs

drop table "reminders";
drop table "scheduledreminders";
drop table "tweets";
drop table "users";

