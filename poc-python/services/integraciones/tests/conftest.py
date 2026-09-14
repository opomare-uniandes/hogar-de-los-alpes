from copy import deepcopy

import pytest


@pytest.fixture
def evento_v1() -> dict:
    return {
        "specversion": "1.0",
        "id": "5cb9dbef-b1ab-46ea-bf96-acf7212ee12d",
        "source": "urn:partner:seguros-andes",
        "type": "com.hda.partner.solicitud-trabajo.v1",
        "subject": "solicitud/SIN-2026-0001",
        "time": 1789322400000,
        "datacontenttype": "application/avro",
        "dataschema": "urn:hda:schema:solicitud-trabajo-partner:v1",
        "correlationId": "5c5889cd-c846-44af-b68a-88f621773ed1",
        "data": {
            "partnerCode": "SEGUROS_ANDES",
            "requestNumber": "SIN-2026-0001",
            "insuredCustomerId": "CLI-991",
            "assistanceCode": "PLUMBING",
            "priorityCode": "P2",
            "cityCode": "BOG",
            "currencyCode": "COP",
            "requestedAt": 1789322400000,
        },
    }


@pytest.fixture
def evento_v2() -> dict:
    return {
        "specversion": "1.0",
        "id": "4b876746-454f-4844-a946-ac5f2ec6fbea",
        "source": "urn:partner:seguros-andes",
        "type": "com.hda.partner.solicitud-trabajo.v2",
        "subject": "solicitud/SIN-2026-0001",
        "time": 1789322400000,
        "datacontenttype": "application/avro",
        "dataschema": "urn:hda:schema:solicitud-trabajo-partner:v2",
        "correlationId": "5c5889cd-c846-44af-b68a-88f621773ed1",
        "data": {
            "partner": {"code": "SEGUROS_ANDES"},
            "request": {
                "number": "SIN-2026-0001",
                "requestedAt": 1789322400000,
            },
            "insured": {"customerId": "CLI-991"},
            "service": {"code": "PLUMBING", "priority": "P2"},
            "location": {"cityCode": "BOG", "countryCode": "CO"},
            "payment": {"currencyCode": "COP"},
        },
    }


@pytest.fixture
def copiar():
    return deepcopy
