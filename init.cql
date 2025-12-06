CREATE KEYSPACE IF NOT EXISTS monitoring WITH replication = {'class':'SimpleStrategy','replication_factor':1};

USE monitoring;

CREATE TABLE IF NOT EXISTS service (
    uuid timeuuid PRIMARY KEY,
    service_name text,
    description text,
    updated_time timestamp,
    email text
);

CREATE TABLE IF NOT EXISTS api_scenario (
    service_uuid timeuuid,
    scenario_uuid timeuuid,
    service_name text,
    url text,
    method text,
    headers map<text, text>,
    requestbody text,
    description text,
    created_time timestamp,
    updated_time timestamp,
PRIMARY KEY (service_uuid, scenario_uuid)
);

CREATE TABLE IF NOT EXISTS tcp_scenario (
    service_uuid timeuuid,
    scenario_uuid timeuuid,
    service_name text,
    ip text,
    port int,
    description text,
    created_time timestamp,
    updated_time timestamp,
PRIMARY KEY (service_uuid, scenario_uuid)
);

USE monitoring;

INSERT INTO service (
    uuid, service_name, description, updated_time, email
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'),
    'user-service',
    'User account management service',
    toTimestamp(now()),
    'choiis1207@gmail.com'
);

INSERT INTO service (
    uuid, service_name, description, updated_time, email
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'),
    'order-service',
    'Order and cart management service',
    toTimestamp(now()),
    'choiis1207@gmail.com'
);

INSERT INTO service (
    uuid, service_name, description, updated_time, email
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'),
    'payment-service',
    'Payment processing service',
    toTimestamp(now()),
    'choiis1207@gmail.com'
);

INSERT INTO service (
    uuid, service_name, description, updated_time, email
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'),
    'notification-service',
    'Email / SMS / push notification service',
    toTimestamp(now()),
    'choiis1207@gmail.com'
);

INSERT INTO service (
    uuid, service_name, description, updated_time, email
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'),
    'reporting-service',
    'Reporting and analytics service',
    toTimestamp(now()),
    'choiis1207@gmail.com'
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://jsonplaceholder.typicode.com/users', 'GET',
    {'Accept':'application/json'},
    '{}',
    'List users (jsonplaceholder)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://jsonplaceholder.typicode.com/users/1', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Get single user (jsonplaceholder)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://jsonplaceholder.typicode.com/users/1/posts', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Get user posts (jsonplaceholder)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://httpbin.org/get?service=user-service', 'GET',
    {'Accept':'application/json'},
    '{}',
    'httpbin GET echo (user-service)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://httpbin.org/anything/user', 'GET',
    {'Accept':'application/json'},
    '{}',
    'httpbin anything endpoint (user)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://httpbin.org/post', 'POST',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "username":"insung", "email":"insung@example.com" }',
    'httpbin POST echo (user create)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://httpbin.org/put', 'PUT',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "field":"value" }',
    'httpbin PUT echo (user update)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://httpbin.org/delete', 'DELETE',
    {'Accept':'application/json'},
    '{}',
    'httpbin DELETE echo (user delete)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://httpbin.org/uuid', 'GET',
    {'Accept':'application/json'},
    '{}',
    'httpbin uuid generator',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    'https://httpbin.org/headers', 'GET',
    {'Accept':'application/json','X-Demo':'user-service'},
    '{}',
    'httpbin header echo',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    '1.1.1.1', 53,
    'Cloudflare DNS (1.1.1.1:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    '1.0.0.1', 53,
    'Cloudflare DNS secondary (1.0.0.1:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    '8.8.8.8', 53,
    'Google DNS (8.8.8.8:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    '8.8.4.4', 53,
    'Google DNS secondary (8.8.4.4:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:00:00Z'), now(), 'user-service',
    '1.1.1.1', 443,
    'Cloudflare HTTPS (1.1.1.1:443)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://jsonplaceholder.typicode.com/posts', 'GET',
    {'Accept':'application/json'},
    '{}',
    'List posts (jsonplaceholder)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://jsonplaceholder.typicode.com/posts/1', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Get single post',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://jsonplaceholder.typicode.com/posts/1/comments', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Get comments for a post',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://jsonplaceholder.typicode.com/comments', 'GET',
    {'Accept':'application/json'},
    '{}',
    'List all comments',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://httpbin.org/get?service=order-service', 'GET',
    {'Accept':'application/json'},
    '{}',
    'httpbin GET (order-service)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://httpbin.org/status/200', 'GET',
    {'Accept':'application/json'},
    '{}',
    'httpbin status=200',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://httpbin.org/status/404', 'GET',
    {'Accept':'application/json'},
    '{}',
    'httpbin status=404',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://httpbin.org/anything/orders', 'POST',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "orderId":123, "amount":10000 }',
    'Fake order POST via httpbin',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://httpbin.org/delay/1', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Delayed response 1s (order)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    'https://httpbin.org/response-headers?X-Order-Service=1', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Custom response headers (order)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    '9.9.9.9', 53,
    'Quad9 DNS (9.9.9.9:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    '149.112.112.112', 53,
    'Quad9 DNS secondary (149.112.112.112:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    '208.67.222.222', 53,
    'OpenDNS (208.67.222.222:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    '208.67.220.220', 53,
    'OpenDNS secondary (208.67.220.220:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:01:00Z'), now(), 'order-service',
    '8.8.8.8', 443,
    'Google HTTPS (8.8.8.8:443)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/get?service=payment-service', 'GET',
    {'Accept':'application/json'},
    '{}',
    'payment-service GET test',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/post', 'POST',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "orderId":1, "amount":10000, "method":"CARD" }',
    'Fake payment create POST',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/anything/payments/1', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Get payment detail (fake)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/put', 'PUT',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "status":"CONFIRMED" }',
    'Update payment status (fake)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/anything/refund', 'POST',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "reason":"CUSTOMER_REQUEST" }',
    'Refund payment (fake)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/status/201', 'GET',
    {'Accept':'application/json'},
    '{}',
    'httpbin status=201 (created)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/status/500', 'GET',
    {'Accept':'application/json'},
    '{}',
    'httpbin status=500 (error)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/delay/2', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Slow payment provider simulation (2s)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/bytes/128', 'GET',
    {'Accept':'application/octet-stream'},
    '{}',
    'Random bytes 128',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    'https://httpbin.org/base64/SW5zdW5nUGF5bWVudA==', 'GET',
    {'Accept':'text/plain'},
    '{}',
    'Base64 decoded response (InsungPayment)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    '1.1.1.1', 853,
    'Cloudflare DNS over TLS (1.1.1.1:853)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    '9.9.9.9', 853,
    'Quad9 DNS over TLS (9.9.9.9:853)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    '8.8.8.8', 853,
    'Google DNS over TLS (8.8.8.8:853)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    '1.0.0.1', 443,
    'Cloudflare HTTPS (1.0.0.1:443)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:02:00Z'), now(), 'payment-service',
    '9.9.9.9', 443,
    'Quad9 HTTPS (9.9.9.9:443)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://api.github.com', 'GET',
    {'Accept':'application/vnd.github+json','User-Agent':'monitoring-demo'},
    '{}',
    'GitHub API root',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://api.github.com/repos/github/docs', 'GET',
    {'Accept':'application/vnd.github+json','User-Agent':'monitoring-demo'},
    '{}',
    'Get GitHub repo (github/docs)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://api.github.com/repos/github/docs/issues', 'GET',
    {'Accept':'application/vnd.github+json','User-Agent':'monitoring-demo'},
    '{}',
    'List issues (public)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://httpbin.org/get?service=notification-service', 'GET',
    {'Accept':'application/json'},
    '{}',
    'httpbin GET (notification-service)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://httpbin.org/anything/notifications/email', 'POST',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "email":"test@example.com", "subject":"hello" }',
    'Fake email send via httpbin',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://httpbin.org/anything/notifications/sms', 'POST',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "phone":"01000000000", "text":"sms message" }',
    'Fake SMS send via httpbin',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://httpbin.org/anything/notifications/push', 'POST',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "deviceToken":"demo-token", "title":"push", "body":"msg" }',
    'Fake push send via httpbin',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://httpbin.org/anything/subscriptions', 'GET',
    {'Accept':'application/json'},
    '{}',
    'List subscriptions (fake)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://httpbin.org/anything/templates', 'GET',
    {'Accept':'application/json'},
    '{}',
    'List templates (fake)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    'https://httpbin.org/anything/templates/1', 'PUT',
    {'Content-Type':'application/json','Accept':'application/json'},
    '{ "subject":"updated subject", "body":"updated body" }',
    'Update template (fake)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    '140.82.121.3', 443,
    'GitHub HTTPS (api.github.com IP, may change)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    '1.1.1.1', 80,
    'Cloudflare HTTP (1.1.1.1:80)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    '8.8.8.8', 80,
    'Google HTTP (8.8.8.8:80)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    '208.67.222.222', 443,
    'OpenDNS HTTPS (208.67.222.222:443)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:03:00Z'), now(), 'notification-service',
    '9.9.9.9', 443,
    'Quad9 HTTPS (9.9.9.9:443)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://jsonplaceholder.typicode.com/todos', 'GET',
    {'Accept':'application/json'},
    '{}',
    'List todos (jsonplaceholder)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://jsonplaceholder.typicode.com/albums', 'GET',
    {'Accept':'application/json'},
    '{}',
    'List albums (jsonplaceholder)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://jsonplaceholder.typicode.com/photos', 'GET',
    {'Accept':'application/json'},
    '{}',
    'List photos (jsonplaceholder)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://jsonplaceholder.typicode.com/todos/1', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Get single todo',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://httpbin.org/get?service=reporting-service', 'GET',
    {'Accept':'application/json'},
    '{}',
    'reporting-service GET test',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://httpbin.org/anything/reports/daily', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Fake daily report endpoint',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://httpbin.org/anything/reports/weekly', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Fake weekly report endpoint',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://httpbin.org/anything/reports/monthly', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Fake monthly report endpoint',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://httpbin.org/anything/reports/export', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Fake export report endpoint',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO api_scenario (
    service_uuid, scenario_uuid, service_name, url, method,
    headers, requestbody, description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    'https://httpbin.org/anything/reports/config', 'GET',
    {'Accept':'application/json'},
    '{}',
    'Fake reporting config endpoint',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    '1.1.1.1', 53,
    'Cloudflare DNS (1.1.1.1:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    '8.8.8.8', 53,
    'Google DNS (8.8.8.8:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    '9.9.9.9', 53,
    'Quad9 DNS (9.9.9.9:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    '208.67.222.222', 53,
    'OpenDNS (208.67.222.222:53)',
    toTimestamp(now()), toTimestamp(now())
);

INSERT INTO tcp_scenario (
    service_uuid, scenario_uuid, service_name, ip, port,
    description, created_time, updated_time
) VALUES (
    minTimeuuid('2025-01-01T10:04:00Z'), now(), 'reporting-service',
    '1.0.0.1', 443,
    'Cloudflare HTTPS (1.0.0.1:443)',
    toTimestamp(now()), toTimestamp(now())
);
