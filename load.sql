CREATE SOURCE chats
FROM KAFKA BROKER 'localhost:9092' TOPIC 'chats'
FORMAT TEXT;

CREATE MATERIALIZED VIEW CHAT_ALL AS
    SELECT (text::JSONB)->>'id' as key,
           CAST(CAST((text::JSONB)->>'timestamp' as text) as timestamptz) as timestamp,
           (text::JSONB)->>'username' as username,
           (text::JSONB)->>'supportname' as supportname,
           (text::JSONB)->>'message' as message
    FROM (SELECT * FROM chats);

CREATE MATERIALIZED VIEW CHAT_TOTALS AS
    SELECT key, COUNT(*)
    FROM CHAT_ALL
    GROUP BY key;
