package com.example.lab12_moviles

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val arequipaLocation = LatLng(-16.4040102, -71.559611) // Arequipa, Perú
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(arequipaLocation, 12f)
    }

    // Ejercicio 1: Estado para el tipo de mapa actual y menú desplegable
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }
    var isMapTypeMenuExpanded by remember { mutableStateOf(false) }

    // Ejercicio 2: Obtener ubicación del usuario
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Inicializar Google Maps y cargar icono personalizado
    val customMarkerIcon: BitmapDescriptor? = remember(context) {
        MapsInitializer.initialize(context)
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.rick_marker)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        } catch (e: Exception) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        }
    }

    // Función para obtener la ubicación actual mediante GPS (FusedLocationProviderClient)
    fun fetchUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    userLocation = latLng
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(latLng, 16f),
                            durationMs = 1500
                        )
                    }
                    Toast.makeText(
                        context,
                        "📍 Ubicación detectada:\nLat: ${String.format("%.5f", location.latitude)}, Lng: ${String.format("%.5f", location.longitude)}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "Obteniendo señal GPS... intenta nuevamente", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // Solicitador de permisos en tiempo de ejecución
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            fetchUserLocation()
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // 3.2. Añadir varios marcadores clásicos
    val locations = listOf(
        LatLng(-16.433415, -71.5442652) to "JLByR",
        LatLng(-16.4205151, -71.4945209) to "Paucarpata",
        LatLng(-16.3524187, -71.5675994) to "Zamacola"
    )

    // 4.1. Controlar la cámara programáticamente al inicio
    LaunchedEffect(Unit) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(LatLng(-16.4100, -71.5350), 12.3f),
            durationMs = 2500
        )
    }

    // 5.1. Coordenadas de los polígonos
    val mallAventuraPolygon = listOf(
        LatLng(-16.432292, -71.509145),
        LatLng(-16.432757, -71.509626),
        LatLng(-16.433013, -71.509310),
        LatLng(-16.432566, -71.508853)
    )

    val parqueLambramaniPolygon = listOf(
        LatLng(-16.422704, -71.530830),
        LatLng(-16.422920, -71.531340),
        LatLng(-16.423264, -71.531110),
        LatLng(-16.423050, -71.530600)
    )

    val plazaDeArmasPolygon = listOf(
        LatLng(-16.398866, -71.536961),
        LatLng(-16.398744, -71.536529),
        LatLng(-16.399178, -71.536289),
        LatLng(-16.399299, -71.536721)
    )

    // --- RUTAS DE POLILÍNEAS ---

    val rutaComercial = listOf(
        LatLng(-16.398866, -71.536961),
        LatLng(-16.4040102, -71.559611),
        LatLng(-16.422704, -71.530830),
        LatLng(-16.432292, -71.509145)
    )

    val spansTrafico = listOf(
        StyleSpan(StrokeStyle.colorBuilder(android.graphics.Color.GREEN).build()),
        StyleSpan(StrokeStyle.colorBuilder(android.graphics.Color.RED).build())
    )

    val rutaTuristicaNorte = listOf(
        LatLng(-16.398866, -71.536961),
        LatLng(-16.392000, -71.539500),
        LatLng(-16.386800, -71.542200),
        LatLng(-16.381200, -71.545800)
    )

    val rutaInterdistrital = listOf(
        LatLng(-16.433415, -71.5442652),
        LatLng(-16.4205151, -71.4945209),
        LatLng(-16.4170000, -71.4700000)
    )

    val rutaCicloviaLarga = listOf(
        LatLng(-16.3524187, -71.5675994),
        LatLng(-16.372000, -71.556000),
        LatLng(-16.386800, -71.542200),
        LatLng(-16.4040102, -71.559611),
        LatLng(-16.425000, -71.568000),
        LatLng(-16.445000, -71.580000),
        LatLng(-16.460000, -71.592000)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = currentMapType,
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = hasLocationPermission
            )
        ) {
            // El icono de Rick representa la ubicación del usuario si ya se obtuvo el GPS;
            // de lo contrario, aparece en la ubicación inicial de Arequipa
            val activeRickPosition = userLocation ?: arequipaLocation
            Marker(
                state = rememberMarkerState(position = activeRickPosition),
                icon = customMarkerIcon,
                title = if (userLocation != null) "Mi Ubicación Actual (GPS)" else "Arequipa Centro",
                snippet = if (userLocation != null) {
                    "Lat: ${String.format("%.4f", userLocation!!.latitude)}, Lng: ${String.format("%.4f", userLocation!!.longitude)}"
                } else {
                    "Marcador personalizado Rick"
                }
            )

            // Varios marcadores clásicos de la guía (3.2)
            locations.forEach { (location, name) ->
                Marker(
                    state = rememberMarkerState(position = location),
                    title = name,
                    snippet = "Punto de interés"
                )
            }

            // Polígonos (5.1)
            Polygon(
                points = plazaDeArmasPolygon,
                strokeColor = Color.Red,
                fillColor = Color.Blue.copy(alpha = 0.5f),
                strokeWidth = 5f
            )

            Polygon(
                points = parqueLambramaniPolygon,
                strokeColor = Color.Red,
                fillColor = Color.Blue.copy(alpha = 0.5f),
                strokeWidth = 5f
            )

            Polygon(
                points = mallAventuraPolygon,
                strokeColor = Color.Red,
                fillColor = Color.Blue.copy(alpha = 0.5f),
                strokeWidth = 5f
            )

            // Polilíneas
            Polyline(
                points = rutaComercial,
                spans = spansTrafico,
                width = 16f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND,
                zIndex = 3f,
                clickable = true,
                onClick = {
                    Toast.makeText(
                        context,
                        "Ruta 1 (Comercial): Tráfico Fluido y Congestionado",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )

            Polyline(
                points = rutaTuristicaNorte,
                color = Color(0xFF00B0FF),
                width = 12f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND,
                clickable = true,
                onClick = {
                    Toast.makeText(
                        context,
                        "Ruta 2 (Turística): Plaza de Armas -> Yanahuara -> Cayma",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            Polyline(
                points = rutaInterdistrital,
                color = Color(0xFFFF5722),
                width = 10f,
                geodesic = true,
                clickable = true,
                onClick = {
                    Toast.makeText(
                        context,
                        "Ruta 3 (Interdistrital): JLByR -> Paucarpata -> Sabandía (Geodésica)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            Polyline(
                points = rutaCicloviaLarga,
                color = Color(0xFF00E676),
                width = 15f,
                pattern = listOf(Dot(), Gap(25f)),
                clickable = true,
                onClick = {
                    Toast.makeText(
                        context,
                        "Ruta 4 (Gran Ciclovía): Zamácola -> Yanahuara -> Sachaca -> Tiabaya",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        // --- BOTÓN FLOTANTE SUPERIOR DERECHO Y MENÚ DESPLEGABLE DE TIPOS DE MAPA (EJERCICIO 1) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { isMapTypeMenuExpanded = !isMapTypeMenuExpanded },
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Text(text = "🗺️", fontSize = 24.sp)
                }

                AnimatedVisibility(
                    visible = isMapTypeMenuExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.width(165.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Tipo de Mapa",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 4.dp)
                            )

                            val mapOptions = listOf(
                                Triple(MapType.NORMAL, "Normal", "🗺️"),
                                Triple(MapType.SATELLITE, "Satélite", "🛰️"),
                                Triple(MapType.HYBRID, "Híbrido", "🌐"),
                                Triple(MapType.TERRAIN, "Terreno", "⛰️"),
                                Triple(MapType.NONE, "None", "⬜")
                            )

                            mapOptions.forEach { (type, label, icon) ->
                                val isSelected = currentMapType == type
                                Surface(
                                    onClick = {
                                        currentMapType = type
                                        isMapTypeMenuExpanded = false
                                        Toast.makeText(context, "Modo: $label", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = icon,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(end = 10.dp)
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- BOTÓN INFERIOR FLOTANTE PARA EL EJERCICIO 2: OBTENER MI UBICACIÓN ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (hasLocationPermission) {
                        fetchUserLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                icon = { Text(text = "📍", fontSize = 20.sp) },
                text = {
                    Text(
                        text = if (userLocation != null) "Mi Ubicación Detectada" else "Obtener Mi Ubicación",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }
}
