import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationRow(
    isEmergencyHelpSelected: Boolean,
    onEmergencyHelpClick: () -> Unit,
    onVolunteerServiceClick: () -> Unit,
    modifier: Modifier = Modifier // Add modifier parameter here
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Gray)
    ) {
        // Emergency Help box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (isEmergencyHelpSelected) Color.LightGray else Color.Gray)
                .clickable { onEmergencyHelpClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Emergency Help",
                color = if (isEmergencyHelpSelected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Divider between boxes
        Spacer(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.DarkGray)
        )

        // Volunteer Service box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (!isEmergencyHelpSelected) Color.LightGray else Color.Gray)
                .clickable { onVolunteerServiceClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Volunteer Service",
                color = if (!isEmergencyHelpSelected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
