package org.multipaz.testapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DiagnosticOption
import org.multipaz.claim.Claim
import org.multipaz.claim.MdocClaim
import org.multipaz.claim.VcClaim
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.testapp.DocumentModel
import org.multipaz.compose.claim.RenderClaimValue

@Composable
fun CredentialClaimsViewerScreen(
    documentModel: DocumentModel,
    documentTypeRepository: DocumentTypeRepository,
    documentId: String,
    credentialId: String,
    showToast: (message: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val documentInfo = documentModel.documentInfos[documentId]
    val credential = documentInfo?.credentials?.find { it.identifier == credentialId  }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (credential == null) {
            Text("No credential for documentId ${documentId} credentialId ${credentialId}")
        } else {
            for (claim in credential.getClaims(documentTypeRepository)) {
                Column(
                    Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = claim.displayName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    RenderClaimValue(claim)
                }
            }
        }
    }
}
