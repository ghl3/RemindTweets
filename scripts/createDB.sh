psql postgres postgres <<END_OF_FILE
CREATE ROLE remindtweets LOGIN
END_OF_FILE


psql postgres postgres <<END_OF_FILE
CREATE DATABASE remindtweets
       WITH OWNER = remindtweets
       ENCODING = 'UTF8'
       TABLESPACE = pg_default
       CONNECTION LIMIT = -1;
END_OF_FILE
