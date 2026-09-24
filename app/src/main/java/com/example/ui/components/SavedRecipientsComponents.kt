package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.SavedRecipientEntity
import com.example.ui.theme.*

/**
 * Helper to launch system contact picker and extract (Name, PhoneNumber).
 */
@Composable
fun rememberContactPicker(
    onContactPicked: (name: String, phoneNumber: String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                var retrievedName = ""
                var retrievedPhone = ""
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
                    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            if (numberIdx >= 0) retrievedPhone = cursor.getString(numberIdx) ?: ""
                            if (nameIdx >= 0) retrievedName = cursor.getString(nameIdx) ?: ""
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Fallback lookup if specific phone projection was empty
                if (retrievedPhone.isBlank()) {
                    try {
                        context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                                if (nameIdx >= 0 && retrievedName.isBlank()) {
                                    retrievedName = cursor.getString(nameIdx) ?: ""
                                }
                                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                                val contactId = if (idIdx >= 0) cursor.getString(idIdx) else null
                                if (contactId != null) {
                                    context.contentResolver.query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                        arrayOf(contactId),
                                        null
                                    )?.use { pCursor ->
                                        if (pCursor.moveToFirst()) {
                                            val pIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                            if (pIdx >= 0) retrievedPhone = pCursor.getString(pIdx) ?: ""
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Clean phone number format
                var cleanPhone = retrievedPhone.replace("[^0-9+]".toRegex(), "")
                if (cleanPhone.startsWith("+234")) {
                    cleanPhone = "0" + cleanPhone.removePrefix("+234")
                }

                val finalName = retrievedName.ifBlank { "Contact" }

                if (cleanPhone.isNotBlank()) {
                    Toast.makeText(context, "Selected: $finalName ($cleanPhone)", Toast.LENGTH_SHORT).show()
                    onContactPicked(finalName, cleanPhone)
                } else if (finalName.isNotBlank()) {
                    Toast.makeText(context, "Selected contact: $finalName", Toast.LENGTH_SHORT).show()
                    onContactPicked(finalName, "")
                }
            }
        }
    }

    return remember(launcher) {
        {
            try {
                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                launcher.launch(intent)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                    launcher.launch(fallbackIntent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "Unable to open phone contacts", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

/**
 * Compact Phone Contact Picker Button
 */
@Composable
fun ContactPickerButton(
    onContactPicked: (name: String, phoneNumber: String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Contacts"
) {
    val launchPicker = rememberContactPicker(onContactPicked)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CyberCyan.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { launchPicker() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Contacts,
                contentDescription = "Pick Contact",
                tint = CyberCyan,
                modifier = Modifier.size(14.dp)
            )
            if (label.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

/**
 * Sleek, horizontal 1-tap Beneficiary Phone Number Selection Row.
 */
@Composable
fun QuickRecipientPickerRow(
    recipients: List<SavedRecipientEntity>,
    recipientTypeFilter: String? = null,
    onRecipientSelected: (SavedRecipientEntity) -> Unit,
    onManageClick: () -> Unit,
    onContactPicked: ((name: String, phoneNumber: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val launchPicker = if (onContactPicked != null) rememberContactPicker(onContactPicked) else null

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PeopleAlt,
                    contentDescription = "Saved",
                    tint = CyberCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Saved Beneficiaries",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (launchPicker != null) {
                    Text(
                        text = "📇 Phone Contacts",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { launchPicker() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = "Manage (${recipients.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onManageClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recipients.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (launchPicker != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { launchPicker() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Contacts, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick from Contacts", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onManageClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Beneficiary", style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick "Contacts" Action Chip
                if (launchPicker != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricEmerald.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { launchPicker() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Contacts, contentDescription = "Contacts", tint = ElectricEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Contacts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricEmerald)
                        }
                    }
                }

                // Quick "Add" Action Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onManageClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Add", tint = CyberCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                }

                recipients.forEach { recipient ->
                    val cleanPhone = recipient.identifier.ifBlank { recipient.name }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (recipient.isFavorite) Color(0xFF1E293B) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (recipient.isFavorite) GlowingAmber.copy(alpha = 0.6f) else DarkCardBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRecipientSelected(recipient) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Initial Avatar Badge
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (recipient.isFavorite) GlowingAmber.copy(alpha = 0.25f)
                                        else CyberCyan.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = recipient.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (recipient.isFavorite) GlowingAmber else CyberCyan,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = recipient.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (recipient.isFavorite) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Favorite",
                                            tint = GlowingAmber,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = cleanPhone,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Beneficiaries Management Dialog.
 * Simple, clean: Just Name + Phone Number (+ Contact Picker).
 */
@Composable
fun SavedRecipientsManagerDialog(
    recipients: List<SavedRecipientEntity>,
    onDismiss: () -> Unit,
    onSaveNewRecipient: (name: String, type: String, identifier: String, provider: String, bankAccountName: String?, isFavorite: Boolean) -> Unit,
    onDeleteRecipient: (String) -> Unit,
    onToggleFavorite: (SavedRecipientEntity) -> Unit,
    onSelectRecipientForAction: ((SavedRecipientEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddRecipientModal by remember { mutableStateOf(false) }

    // Direct Contact Picker Launcher to import contact into beneficiaries
    val launchContactPicker = rememberContactPicker { name, phoneNumber ->
        if (phoneNumber.isNotBlank()) {
            onSaveNewRecipient(
                name.ifBlank { "Beneficiary" },
                "contact",
                phoneNumber,
                "",
                null,
                true
            )
            Toast.makeText(context, "Added $name ($phoneNumber) to Beneficiaries!", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredList = remember(recipients, searchQuery) {
        if (searchQuery.isBlank()) recipients
        else {
            recipients.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.identifier.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.PeopleAlt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Saved Beneficiaries",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "${recipients.size} saved phone numbers",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Action Buttons (Import from Phone Contacts & Add Manually)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { launchContactPicker() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Contacts, contentDescription = "Contacts", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import from Contacts", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = { showAddRecipientModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(0.8f).height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name or phone number...", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Beneficiary Contact List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.ContactPhone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No beneficiaries saved yet", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                            Text("Import from phone contacts or save after buying a service.", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { launchContactPicker() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick from Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { recipient ->
                            SavedRecipientCard(
                                recipient = recipient,
                                onSelect = {
                                    onSelectRecipientForAction?.invoke(recipient)
                                    onDismiss()
                                },
                                onToggleFavorite = { onToggleFavorite(recipient) },
                                onDelete = { onDeleteRecipient(recipient.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to Add New Recipient manually (Simple: Name + Phone Number)
    if (showAddRecipientModal) {
        AddNewRecipientDialog(
            onDismiss = { showAddRecipientModal = false },
            onSave = { name, type, identifier, provider, acctName, fav ->
                onSaveNewRecipient(name, type, identifier, provider, acctName, fav)
                showAddRecipientModal = false
            }
        )
    }
}

/**
 * Clean Beneficiary Card (Avatar + Name + Phone Number + Favorite Toggle + Delete).
 */
@Composable
fun SavedRecipientCard(
    recipient: SavedRecipientEntity,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val cleanPhone = recipient.identifier.ifBlank { "No Number" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (recipient.isFavorite) GlowingAmber.copy(alpha = 0.5f) else DarkCardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Initial Letter Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (recipient.isFavorite) GlowingAmber.copy(alpha = 0.25f)
                            else CyberCyan.copy(alpha = 0.25f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = recipient.name.take(1).uppercase().ifBlank { "C" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (recipient.isFavorite) GlowingAmber else CyberCyan,
                            fontSize = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recipient.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (recipient.isFavorite) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = GlowingAmber,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = cleanPhone,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Favorite Toggle Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (recipient.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (recipient.isFavorite) GlowingAmber else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Beneficiary?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Are you sure you want to remove '${recipient.name}' ($cleanPhone) from your saved beneficiaries?", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                        Toast.makeText(context, "Beneficiary removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

/**
 * Simple Dialog to Add a Beneficiary: Just Name + Phone Number (+ Contact Picker)
 */
@Composable
fun AddNewRecipientDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, identifier: String, provider: String, bankAccountName: String?, isFavorite: Boolean) -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(true) }

    val launchPicker = rememberContactPicker { pickedName, pickedPhone ->
        if (pickedName.isNotBlank()) nameInput = pickedName
        if (pickedPhone.isNotBlank()) phoneInput = pickedPhone
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Beneficiary", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color.White))
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Import from Phone Contacts Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricEmerald.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { launchPicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Contacts, contentDescription = null, tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick from Phone Contacts", style = MaterialTheme.typography.labelSmall.copy(color = ElectricEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recipient Name
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Contact Name (e.g. Mom, Sunday)") },
                    placeholder = { Text("Enter name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Number
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("080XXXXXXXX") },
                    trailingIcon = {
                        IconButton(onClick = { launchPicker() }) {
                            Icon(imageVector = Icons.Default.ContactPhone, contentDescription = "Contacts", tint = CyberCyan, modifier = Modifier.size(18.dp))
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Favorite Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isFavorite = !isFavorite }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        colors = CheckboxDefaults.colors(checkedColor = GlowingAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pin to Favorites (Top of Quick-Fill Row)", fontSize = 12.sp, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Action Button
                Button(
                    onClick = {
                        val cleanPhone = phoneInput.replace("[^0-9+]".toRegex(), "").trim()
                        if (cleanPhone.isBlank()) {
                            Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val finalName = nameInput.trim().ifBlank { "Beneficiary" }
                        onSave(
                            finalName,
                            "contact",
                            cleanPhone,
                            "",
                            null,
                            isFavorite
                        )
                        Toast.makeText(context, "Beneficiary saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkObsidian),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Beneficiary", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}
