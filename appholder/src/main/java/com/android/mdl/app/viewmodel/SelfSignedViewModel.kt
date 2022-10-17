package com.android.mdl.app.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.mdl.app.R
import com.android.mdl.app.document.DocumentManager
import com.android.mdl.app.util.DocumentData.MDL_DOCTYPE
import com.android.mdl.app.util.DocumentData.MICOV_DOCTYPE
import com.android.mdl.app.util.DocumentData.MVR_DOCTYPE
import com.android.mdl.app.util.Field
import com.android.mdl.app.util.FieldType
import com.android.mdl.app.util.SelfSignedDocumentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelfSignedViewModel(val app: Application) :
    AndroidViewModel(app) {

    companion object {
        private const val LOG_TAG = "SelfSignedViewModel"
    }

    private val documentManager = DocumentManager.getInstance(app.applicationContext)
    val loading = MutableLiveData<Int>()
    val created = MutableLiveData<Boolean>()
    private val fieldsMdl = mutableListOf<Field>()
    private val fieldsMvr = mutableListOf<Field>()
    private val fieldsMicov = mutableListOf<Field>()

    init {
        loading.value = View.GONE
        var id = 1

        // Pre fill default values for MDL document
        fieldsMdl.add(Field(id++, "Document Name", "document_name", FieldType.STRING, "Driving License"))
        val bitmap = BitmapFactory.decodeResource(
            app.resources,
            R.drawable.img_erika_portrait
        )
        val signature = BitmapFactory.decodeResource(
            app.resources,
            R.drawable.img_erika_signature
        )
        fieldsMdl.add(Field(id++, "Portrait", "portrait", FieldType.BITMAP, bitmap))
        fieldsMdl.add(Field(id++, "Given Name", "given_name", FieldType.STRING, "Erika"))
        fieldsMdl.add(Field(id++, "Family Name", "family_name", FieldType.STRING, "Mustermann"))
        fieldsMdl.add(Field(id++, "Birth Date", "birth_date", FieldType.DATE, "1971-09-01"))
        fieldsMdl.add(Field(id++, "Issue Date", "issue_date", FieldType.DATE, "2021-04-18"))
        fieldsMdl.add(Field(id++, "Expiry Date", "expiry_date", FieldType.DATE, "2026-04-18"))
        fieldsMdl.add(Field(id++, "Issuing Country", "issuing_country", FieldType.STRING, "UT"))
        fieldsMdl.add(Field(id++, "Issuing Authority", "issuing_authority", FieldType.STRING, "Google"))
        fieldsMdl.add(Field(id++, "Document Number", "document_number", FieldType.STRING, "987654321"))
        fieldsMdl.add(Field(id++, "Signature", "signature_usual_mark", FieldType.BITMAP, signature))
        fieldsMdl.add(Field(id++, "UN Distinguishing Sign", "un_distinguishing_sign", FieldType.STRING, "UT"))
        fieldsMdl.add(Field(id++, "Age Over 18", "age_over_18", FieldType.BOOLEAN, "true"))
        fieldsMdl.add(Field(id++, "Age Over 21", "age_over_21", FieldType.BOOLEAN, "true"))
        // TODO: Add driving_privileges dynamically
        fieldsMdl.add(Field(id++, "vehicle Category Code 1", "vehicle_category_code_1", FieldType.STRING, "A"))
        fieldsMdl.add(Field(id++, "Issue Date 1", "issue_date_1", FieldType.DATE, "2018-08-09"))
        fieldsMdl.add(Field(id++, "Expiry Date 1", "expiry_date_1", FieldType.DATE, "2024-10-20"))
        fieldsMdl.add(Field(id++, "vehicle Category Code 2", "vehicle_category_code_2", FieldType.STRING, "B"))
        fieldsMdl.add(Field(id++, "Issue Date 2", "issue_date_2", FieldType.DATE, "2017-02-23"))
        fieldsMdl.add(Field(id++, "Expiry Date 2", "expiry_date_2", FieldType.DATE, "2024-10-20"))
        fieldsMdl.add(Field(id, "Real Id", "real_id", FieldType.BOOLEAN, "true"))

        // Pre fill default values for MRV document
        id = 1
        fieldsMvr.add(Field(id++, "Document Name", "document_name", FieldType.STRING, "Vehicle Registration"))
        fieldsMvr.add(Field(id++, "Issuing Country", "issuingCountry", FieldType.STRING, "UT"))
        fieldsMvr.add(Field(id++, "Competent Authority", "competentAuthority", FieldType.STRING, "RDW"))
        fieldsMvr.add(Field(id++, "Registration Number", "registrationNumber", FieldType.STRING, "E-01-23"))
        fieldsMvr.add(Field(id++, "Issue Date", "issueDate", FieldType.DATE, "2021-04-18"))
        fieldsMvr.add(Field(id++, "Valid From", "validFrom", FieldType.DATE, "2021-04-19"))
        fieldsMvr.add(Field(id++, "Valid Until", "validUntil", FieldType.DATE, "2023-04-20"))
        fieldsMvr.add(Field(id++, "Issue Date", "issue_date", FieldType.DATE, "2021-04-18"))
        fieldsMvr.add(Field(id++, "Registration Holder Name", "name", FieldType.STRING, "Sample Name"))
        fieldsMvr.add(Field(id++, "Street Name", "streetName", FieldType.STRING, "teststraat"))
        fieldsMvr.add(Field(id++, "House Number", "houseNumber", FieldType.STRING, "86"))
        fieldsMvr.add(Field(id++, "Postal Code", "postalCode", FieldType.STRING, "1234 AA"))
        fieldsMvr.add(Field(id++, "Place Of Residence", "placeOfResidence", FieldType.STRING, "Samplecity"))
        fieldsMvr.add(Field(id++, "Ownership Status", "ownershipStatus", FieldType.STRING, "2"))
        fieldsMvr.add(Field(id++, "Vehicle Maker", "make", FieldType.STRING, "Dummymobile"))
        fieldsMvr.add(Field(id, "Vehicle Identification Number", "vin", FieldType.STRING, "1M8GDM9AXKP042788"))

        // Pre fill default values for MICOV document
        id = 1
        fieldsMicov.add(Field(id++, "Document Name", "document_name", FieldType.STRING, "Vaccination Document"))
        fieldsMicov.add(Field(id++, "Family name initial", "fni", FieldType.STRING, "M"))
        fieldsMicov.add(Field(id++, "Family name", "fn", FieldType.STRING, "Mustermann"))
        fieldsMicov.add(Field(id++, "Given name initial", "gni", FieldType.STRING, "E"))
        fieldsMicov.add(Field(id++, "Given name", "gn", FieldType.STRING, "Erika"))
        fieldsMicov.add(Field(id++, "Date of birth", "dob", FieldType.DATE, "1964-08-12"))
        fieldsMicov.add(Field(id++, "Sex", "sex", FieldType.STRING, "2"))
        fieldsMicov.add(Field(id++, "Yellow fever vaccinated", "1D47_vaccinated", FieldType.STRING, "2"))
        fieldsMicov.add(Field(id++, "COVID-19 vaccinated", "RA01_vaccinated", FieldType.STRING, "2"))
        fieldsMicov.add(Field(id++, "Facial image", "fac", FieldType.BITMAP, bitmap))
        fieldsMicov.add(Field(id++, "Family name initial", "fni", FieldType.STRING, "M"))
        fieldsMicov.add(Field(id++, "Birth year", "by", FieldType.STRING, "1964"))
        fieldsMicov.add(Field(id++, "Birth month", "bm", FieldType.STRING, "8"))
        fieldsMicov.add(Field(id++, "Birth day", "bd", FieldType.STRING, "12"))

        fieldsMicov.add(Field(id++, "RA01 1 Disease or agent targeted", "RA01_1_tg", FieldType.STRING, "840539006"))
        fieldsMicov.add(Field(id++, "RA01 1 Vaccine or prophylaxis", "RA01_1_vp", FieldType.STRING, "1119349007"))
        fieldsMicov.add(Field(id++, "RA01 1 Vaccine medicinal product", "RA01_1_mp", FieldType.STRING, "EU/1/20/1528"))
        fieldsMicov.add(Field(id++, "RA01 1 Manufacturer", "RA01_1_ma", FieldType.STRING, "ORG-100030215"))
        fieldsMicov.add(Field(id++, "RA01 1 Batch number", "RA01_1_bn", FieldType.STRING, "B12345/67"))
        fieldsMicov.add(Field(id++, "RA01 1 Dose number", "RA01_1_dn", FieldType.STRING, "1"))
        fieldsMicov.add(Field(id++, "RA01 1 Total series of doses", "RA01_1_sd", FieldType.STRING, "2"))
        fieldsMicov.add(Field(id++, "RA01 1 Date of vaccination", "RA01_1_dt", FieldType.DATE, "2021-04-08"))
        fieldsMicov.add(Field(id++, "RA01 1 Country of vaccination", "RA01_1_co", FieldType.STRING, "UT"))
        fieldsMicov.add(Field(id++, "RA01 1 Administering organization", "RA01_1_ao", FieldType.STRING, "RHI"))
        fieldsMicov.add(Field(id++, "RA01 1 Due date of next dose", "RA01_1_nx", FieldType.STRING, "2021-05-20"))
        fieldsMicov.add(Field(id++, "RA01 1 Certificate issuer", "RA01_1_is", FieldType.STRING, "SC17"))
        fieldsMicov.add(Field(id++, "RA01 1 Unique certificate identifier (UVCI)", "RA01_1_ci", FieldType.STRING, "URN:UVCI:01:UT:187/37512422923"))

        fieldsMicov.add(Field(id++, "RA01 2 Disease or agent targeted", "RA01_2_tg", FieldType.STRING, "840539006"))
        fieldsMicov.add(Field(id++, "RA01 2 Vaccine or prophylaxis", "RA01_2_vp", FieldType.STRING, "1119349007"))
        fieldsMicov.add(Field(id++, "RA01 2 Vaccine medicinal product", "RA01_2_mp", FieldType.STRING, "EU/1/20/1528"))
        fieldsMicov.add(Field(id++, "RA01 2 Manufacturer", "RA01_2_ma", FieldType.STRING, "ORG-100030215"))
        fieldsMicov.add(Field(id++, "RA01 2 Batch number", "RA01_2_bn", FieldType.STRING, "B12345/67"))
        fieldsMicov.add(Field(id++, "RA01 2 Dose number", "RA01_2_dn", FieldType.STRING, "2"))
        fieldsMicov.add(Field(id++, "RA01 2 Total series of doses", "RA01_2_sd", FieldType.STRING, "2"))
        fieldsMicov.add(Field(id++, "RA01 2 Date of vaccination", "RA01_2_dt", FieldType.DATE, "2021-05-18"))
        fieldsMicov.add(Field(id++, "RA01 2 Country of vaccination", "RA01_2_co", FieldType.STRING, "UT"))
        fieldsMicov.add(Field(id++, "RA01 2 Administering organization", "RA01_2_ao", FieldType.STRING, "RHI"))
        fieldsMicov.add(Field(id++, "RA01 2 Certificate issuer", "RA01_2_is", FieldType.STRING, "SC17"))
        fieldsMicov.add(Field(id++, "RA01 2 Unique certificate identifier (UVCI)", "RA01_2_ci", FieldType.STRING, "URN:UVCI:01:UT:187/37512533044"))

        fieldsMicov.add(Field(id++, "Type of person identifier", "PPN_pty", FieldType.STRING, "PPN"))
        fieldsMicov.add(Field(id++, "Unique number", "PPN_pnr", FieldType.STRING, "476284728"))
        fieldsMicov.add(Field(id++, "pic", "PPN_pic", FieldType.STRING, "UT"))

        fieldsMicov.add(Field(id++, "Type of person identifier", "DL_pty", FieldType.STRING, "DL"))
        fieldsMicov.add(Field(id++, "Unique number", "DL_pnr", FieldType.STRING, "987654321"))
        fieldsMicov.add(Field(id++, "pic", "DL_pic", FieldType.STRING, "UT"))

        fieldsMicov.add(Field(id++, "Test Result", "Result", FieldType.STRING, "260415000"))
        fieldsMicov.add(Field(id++, "Type Of Test", "TypeOfTest", FieldType.STRING, "LP6464-4"))
        fieldsMicov.add(Field(id++, "Time Of Test", "TimeOfTest", FieldType.DATE, "2021-10-12"))

        fieldsMicov.add(Field(id++, "SafeEntry", "SeCondFulfilled", FieldType.STRING, "1"))
        fieldsMicov.add(Field(id++, "SafeEntry type", "SeCondType", FieldType.STRING, "leisure"))
        fieldsMicov.add(Field(id, "SafeEntry expiry", "SeCondExpiry", FieldType.DATE, "2021-10-13"))
    }

    fun getFields(docType: String): MutableList<Field> {
        return if (MDL_DOCTYPE == docType) {
            fieldsMdl
        } else if (MVR_DOCTYPE == docType) {
            fieldsMvr
        } else if (MICOV_DOCTYPE == docType) {
            fieldsMicov
        } else {
            throw IllegalArgumentException("No field list valid for $docType")
        }
    }

    fun createSelfSigned(dData: SelfSignedDocumentData) {
        loading.value = View.VISIBLE
        viewModelScope.launch (Dispatchers.IO) {
            documentManager.createSelfSignedCredential(dData)
            withContext(Dispatchers.Main) {
                created.value = true
                loading.value = View.GONE
            }
        }
    }

}

