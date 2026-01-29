package contracts.alert

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "should_accept_alert"
    description "Should accept alert request and return 202 Accepted"

    request {
        method POST()
        url "/api/v1/alert"
        headers {
            contentType applicationJson()
        }
        body([
            serviceUuid: $(consumer(regex('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')), producer('550e8400-e29b-41d4-a716-446655440000')),
            scenarioUuid: $(consumer(regex('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')), producer('660e8400-e29b-41d4-a716-446655440001')),
            serviceName: $(consumer(regex('.+')), producer('test-service'))
        ])
    }

    response {
        status ACCEPTED()
    }
}
