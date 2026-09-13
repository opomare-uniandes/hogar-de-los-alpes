from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class SolicitudEnvelopeBase(BaseModel):
    """Atributos comunes del sobre de integracion basado en CloudEvents."""

    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    specversion: Literal["1.0"] = "1.0"
    id: UUID
    source: str = Field(min_length=1)
    event_type: str = Field(alias="type", min_length=1)
    subject: str = Field(min_length=1)
    time: int = Field(ge=0)
    datacontenttype: Literal["application/avro"] = "application/avro"
    dataschema: str = Field(min_length=1)
    correlation_id: UUID = Field(alias="correlationId")


class SolicitudTrabajoDataV1(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    partner_code: str = Field(alias="partnerCode", min_length=1)
    request_number: str = Field(alias="requestNumber", min_length=1)
    insured_customer_id: str = Field(alias="insuredCustomerId", min_length=1)
    assistance_code: str = Field(alias="assistanceCode", min_length=1)
    priority_code: str = Field(alias="priorityCode", min_length=1)
    city_code: str = Field(alias="cityCode", min_length=1)
    currency_code: str = Field(alias="currencyCode", min_length=3, max_length=3)
    requested_at: int = Field(alias="requestedAt", ge=0)


class SolicitudTrabajoPartnerV1Create(SolicitudEnvelopeBase):
    """Contrato de entrada requerido para crear una solicitud V1."""

    event_type: Literal["com.hda.partner.solicitud-trabajo.v1"] = Field(alias="type")
    data: SolicitudTrabajoDataV1


class PartnerReferenceV2(BaseModel):
    model_config = ConfigDict(extra="forbid")
    code: str = Field(min_length=1)


class RequestReferenceV2(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")
    number: str = Field(min_length=1)
    requested_at: int = Field(alias="requestedAt", ge=0)


class InsuredReferenceV2(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")
    customer_id: str = Field(alias="customerId", min_length=1)


class RequestedServiceV2(BaseModel):
    model_config = ConfigDict(extra="forbid")
    code: str = Field(min_length=1)
    priority: str = Field(min_length=1)


class ServiceLocationV2(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")
    city_code: str = Field(alias="cityCode", min_length=1)
    country_code: str = Field(alias="countryCode", min_length=2, max_length=2)


class PaymentReferenceV2(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")
    currency_code: str = Field(alias="currencyCode", min_length=3, max_length=3)


class SolicitudTrabajoDataV2(BaseModel):
    model_config = ConfigDict(extra="forbid")

    partner: PartnerReferenceV2
    request: RequestReferenceV2
    insured: InsuredReferenceV2
    service: RequestedServiceV2
    location: ServiceLocationV2
    payment: PaymentReferenceV2


class SolicitudTrabajoPartnerV2Create(SolicitudEnvelopeBase):
    """Contrato de entrada requerido para crear una solicitud V2."""

    event_type: Literal["com.hda.partner.solicitud-trabajo.v2"] = Field(alias="type")
    data: SolicitudTrabajoDataV2

