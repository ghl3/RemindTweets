# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "reminders" ("id" SERIAL NOT NULL PRIMARY KEY,"user_id" BIGINT NOT NULL,"twitter_id" BIGINT NOT NULL,"createdat" timestamptz NOT NULL,"repeat" repeat NOT NULL,"firsttime" timestamptz NOT NULL,"what" VARCHAR(254) NOT NULL,"tweet_id" BIGINT NOT NULL);
create unique index "UNIQUE_REMINDER_TWITTERID" on "reminders" ("twitter_id");
create table "scheduledreminders" ("id" SERIAL NOT NULL PRIMARY KEY,"reminder_id" BIGINT NOT NULL,"user_id" BIGINT NOT NULL,"time" timestamptz NOT NULL,"executed" BOOLEAN NOT NULL,"cancelled" BOOLEAN NOT NULL,"in_progress" BOOLEAN NOT NULL,"failed" BOOLEAN NOT NULL);
create table "tweets" ("id" SERIAL NOT NULL PRIMARY KEY,"user_id" BIGINT NOT NULL,"twitter_id" BIGINT NOT NULL,"screen_name" VARCHAR(254) NOT NULL,"content" json NOT NULL,"fetchedat" timestamptz NOT NULL);
create unique index "UNIQUE_TWEET_TWITTERID" on "tweets" ("twitter_id");
create table "users" ("id" SERIAL NOT NULL PRIMARY KEY,"screen_name" VARCHAR(254) NOT NULL,"createdat" timestamp NOT NULL);
alter table "reminders" add constraint "TWEET_USER_FK" foreign key("user_id") references "users"("id") on update NO ACTION on delete NO ACTION;
alter table "reminders" add constraint "REMINDER_TWEET_FK" foreign key("twitter_id") references "tweets"("id") on update NO ACTION on delete NO ACTION;
alter table "scheduledreminders" add constraint "SCHEDULEDREMINDER_USER_FK" foreign key("user_id") references "users"("id") on update NO ACTION on delete NO ACTION;
alter table "scheduledreminders" add constraint "SCHEDULEDREMINDER_REMINDER_FK" foreign key("reminder_id") references "reminders"("id") on update NO ACTION on delete NO ACTION;
alter table "tweets" add constraint "TWEET_USER_FK" foreign key("user_id") references "users"("id") on update NO ACTION on delete NO ACTION;

# --- !Downs

alter table "reminders" drop constraint "TWEET_USER_FK";
alter table "reminders" drop constraint "REMINDER_TWEET_FK";
alter table "scheduledreminders" drop constraint "SCHEDULEDREMINDER_USER_FK";
alter table "scheduledreminders" drop constraint "SCHEDULEDREMINDER_REMINDER_FK";
alter table "tweets" drop constraint "TWEET_USER_FK";
drop table "reminders";
drop table "scheduledreminders";
drop table "tweets";
drop table "users";

