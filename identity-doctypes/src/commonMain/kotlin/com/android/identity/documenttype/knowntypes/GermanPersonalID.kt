package com.android.identity.documenttype.knowntypes

import com.android.identity.cbor.CborArray
import com.android.identity.cbor.toDataItem
import com.android.identity.cbor.toDataItemFullDate
import com.android.identity.documenttype.DocumentAttributeType
import com.android.identity.documenttype.DocumentType
import com.android.identity.documenttype.Icon
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Object containing the metadata of the German ID Document Type.
 *
 * For now, this is a copy of EUPersonaID.
 *
 * TODO: read this (and other) VCTs for their URLs.
 */
object GermanPersonalID {
    const val EUPID_VCT = "https://example.bmi.bund.de/credential/pid/1.0"

    /**
     * Build the EU Personal ID Document Type.
     */
    fun getDocumentType(): DocumentType {
        return DocumentType.Builder("German Personal ID")
            .addVcDocumentType(EUPID_VCT)
            .addVcAttribute(
                DocumentAttributeType.String,
                "family_name",
                "Family Name",
                "Current last name(s), surname(s), or primary identifier of the PID holder",
                Icon.PERSON,
                JsonPrimitive(SampleData.FAMILY_NAME)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "given_name",
                "Given Names",
                "Current first name(s), other name(s), or secondary identifier of the PID holder",
                Icon.PERSON,
                JsonPrimitive(SampleData.GIVEN_NAME)
            )
            .addVcAttribute(
                DocumentAttributeType.Date,
                "birthdate",
                "Date of Birth",
                "Day, month, and year on which the PID holder was born. If unknown, approximate date of birth.",
                Icon.TODAY,
                JsonPrimitive(SampleData.BIRTH_DATE)
            )
            .addVcAttribute(
                DocumentAttributeType.Number,
                "age_in_years",
                "Age in Years",
                "The age of the PID holder in years",
                Icon.TODAY,
                JsonPrimitive(SampleData.AGE_IN_YEARS)
            )
            .addVcAttribute(
                DocumentAttributeType.Number,
                "age_birth_year",
                "Year of Birth",
                "The year when the PID holder was born",
                Icon.TODAY,
                JsonPrimitive(SampleData.AGE_BIRTH_YEAR)
            )
            .addVcAttribute(
                DocumentAttributeType.Boolean,
                "12",
                "Older Than 12",
                "Age over 12?",
                Icon.TODAY,
                JsonPrimitive(SampleData.AGE_OVER)
            )
            .addVcAttribute(
                DocumentAttributeType.Boolean,
                "14",
                "Older Than 14",
                "Age over 14?",
                Icon.TODAY,
                JsonPrimitive(SampleData.AGE_OVER)
            )
            .addVcAttribute(
                DocumentAttributeType.Boolean,
                "16",
                "Older Than 16",
                "Age over 16?",
                Icon.TODAY,
                JsonPrimitive(SampleData.AGE_OVER_16)
            )
            // TODO: nest in age_equal_or_over object
            .addVcAttribute(
                DocumentAttributeType.Boolean,
                "18",
                "Older Than 18",
                "Age over 18?",
                Icon.TODAY,
                JsonPrimitive(SampleData.AGE_OVER_18)
            )
            .addVcAttribute(
                DocumentAttributeType.Boolean,
                "21",
                "Older Than 21",
                "Age over 21?",
                Icon.TODAY,
                JsonPrimitive(SampleData.AGE_OVER_21)
            )
            .addVcAttribute(
                DocumentAttributeType.Boolean,
                "65",
                "Older Than 65",
                "Age over 65?",
                Icon.TODAY,
                JsonPrimitive(SampleData.AGE_OVER_65)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "birth_family_name",
                "Family Name at Birth",
                "Last name(s), surname(s), or primary identifier of the PID holder at birth",
                Icon.PERSON,
                JsonPrimitive(SampleData.FAMILY_NAME_BIRTH)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "birth_place",
                "Place of Birth",
                "Country and municipality or state/province where the PID holder was born",
                Icon.PLACE,
                JsonPrimitive(SampleData.BIRTH_PLACE)
            )
            .addVcAttribute(
                DocumentAttributeType.StringOptions(Options.COUNTRY_ISO_3166_1_ALPHA_2),
                "birth_country",
                "Country of Birth",
                "The country where the PID User was born, as an Alpha-2 country code as specified in ISO 3166-1",
                Icon.PLACE,
                JsonPrimitive(SampleData.BIRTH_COUNTRY)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "birth_state",
                "State of Birth",
                "The state, province, district, or local area where the PID User was born",
                Icon.PLACE,
                JsonPrimitive(SampleData.BIRTH_STATE)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "birth_city",
                "City of Birth",
                "The municipality, city, town, or village where the PID User was born",
                Icon.PLACE,
                JsonPrimitive(SampleData.BIRTH_CITY)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "street_address",
                "Resident Address",
                "The full address of the place where the PID holder currently resides and/or may be contacted (street/house number, municipality etc.)",
                Icon.PLACE,
                JsonPrimitive(SampleData.RESIDENT_ADDRESS)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "locality",
                "Resident City",
                "The city where the PID holder currently resides",
                Icon.PLACE,
                JsonPrimitive(SampleData.RESIDENT_CITY)
            )
            .addVcAttribute(
                DocumentAttributeType.StringOptions(Options.COUNTRY_ISO_3166_1_ALPHA_2),
                "country",
                "Resident Country",
                "The country where the PID User currently resides, as an Alpha-2 country code as specified in ISO 3166-1",
                Icon.PLACE,
                JsonPrimitive(SampleData.RESIDENT_COUNTRY)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "postal_code",
                "Resident Postal Code",
                "The postal code of the place where the PID holder currently resides",
                Icon.PLACE,
                JsonPrimitive(SampleData.RESIDENT_POSTAL_CODE)
            )
            .addVcAttribute(
                DocumentAttributeType.IntegerOptions(Options.SEX_ISO_IEC_5218),
                "gender",
                "Gender",
                "PID holder’s gender",
                Icon.EMERGENCY,
                JsonPrimitive(SampleData.SEX_ISO218)
            )
            .addVcAttribute(
                DocumentAttributeType.ComplexType,
                "nationalities",
                "Nationality",
                "List of Alpha-2 country codes as specified in ISO 3166-1, representing the nationality of the PID User.",
                Icon.LANGUAGE,
                buildJsonArray {
                    add(JsonPrimitive(SampleData.NATIONALITY))
                }
            )
            .addVcAttribute(
                DocumentAttributeType.Date,
                "issuance_date",
                "Date of Issue",
                "Date (and possibly time) when the PID was issued.",
                Icon.DATE_RANGE,
                JsonPrimitive(SampleData.ISSUE_DATE)
            )
            .addVcAttribute(
                DocumentAttributeType.Date,
                "expiry_date",
                "Date of Expiry",
                "Date (and possibly time) when the PID will expire.",
                Icon.CALENDAR_CLOCK,
                JsonPrimitive(SampleData.EXPIRY_DATE)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "issuing_authority",
                "Issuing Authority",
                "Name of the administrative authority that has issued this PID instance, or the " +
                        "ISO 3166 Alpha-2 country code of the respective Member State if there is" +
                        "no separate authority authorized to issue PIDs.",
                Icon.ACCOUNT_BALANCE,
                JsonPrimitive(SampleData.ISSUING_AUTHORITY_EU_PID)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "document_number",
                "Document Number",
                "A number for the PID, assigned by the PID Provider.",
                Icon.NUMBERS,
                JsonPrimitive(SampleData.DOCUMENT_NUMBER)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "administrative_number",
                "Administrative Number",
                "A number assigned by the PID Provider for audit control or other purposes.",
                Icon.NUMBERS,
                JsonPrimitive(SampleData.ADMINISTRATIVE_NUMBER)
            )
            .addVcAttribute(
                DocumentAttributeType.String,
                "issuing_jurisdiction",
                "Issuing Jurisdiction",
                "Country subdivision code of the jurisdiction that issued the PID, as defined in " +
                        "ISO 3166-2:2020, Clause 8. The first part of the code SHALL be the same " +
                        "as the value for issuing_country.",
                Icon.ACCOUNT_BALANCE,
                JsonPrimitive(SampleData.ISSUING_JURISDICTION)
            )
            .addVcAttribute(
                DocumentAttributeType.StringOptions(Options.COUNTRY_ISO_3166_1_ALPHA_2),
                "issuing_country",
                "Issuing Country",
                "Alpha-2 country code, as defined in ISO 3166-1, of the issuing authority’s " +
                        "country or territory",
                Icon.ACCOUNT_BALANCE,
                JsonPrimitive(SampleData.ISSUING_COUNTRY)
            )
            .addSampleRequest(
                id = "age_over_18",
                displayName = "Age Over 18",
                vcClaims = listOf("18")
            )
            .addSampleRequest(
                id = "mandatory",
                displayName = "Mandatory Data Elements",
                vcClaims = listOf(
                    "family_name",
                    "given_name",
                    "birthdate",
                    "18",
                    "issuance_date",
                    "expiry_date",
                    "issuing_authority",
                    "issuing_country"
                )
            )
            .addSampleRequest(
                id = "full",
                displayName = "All Data Elements",
                vcClaims = listOf()
            )
            .build()
    }
}