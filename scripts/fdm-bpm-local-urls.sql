UPDATE processes.application_type_enum
SET target_call = 'http://capability-backend:8080/api/v1/business-capability/public/{id}'
WHERE alias IN ('create_business_capability', 'update_business_capability');
