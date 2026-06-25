// =====================================================
// LABERINTO INTELIGENTE - CÓDIGO ARDUINO COMPLETO
// =====================================================

// Pines del Joystick
const int pinJoyX = A0;
const int pinJoyY = A1;
const int pinJoySW = 2;

// Pines del Display 7 Segmentos (Ánodo Común)
const int pinSegA = 3;
const int pinSegB = 4;
const int pinSegC = 5;
const int pinSegD = 6;
const int pinSegE = 7;
const int pinSegF = 8;
const int pinSegG = 9;
const int pinSegDP = 10;

// Pines del Buzzer
const int pinBuzzer = 12;

// Variables globales de estado
int nivelActual = 0;
bool muteActivo = false;
int volumen = 50;

// Mapa del display 7 segmentos (Ánodo Común: 0 = Encendido, 1 = Apagado)
const int numeros[10][8] = {
  {0, 0, 0, 0, 0, 0, 1, 1},  // 0
  {1, 0, 0, 1, 1, 1, 1, 1},  // 1
  {0, 0, 1, 0, 0, 1, 0, 1},  // 2
  {0, 0, 0, 0, 1, 1, 0, 1},  // 3
  {1, 0, 0, 1, 1, 0, 0, 1},  // 4
  {0, 1, 0, 0, 1, 0, 0, 1},  // 5
  {0, 1, 0, 0, 0, 0, 0, 1},  // 6
  {0, 0, 0, 1, 1, 1, 1, 1},  // 7
  {0, 0, 0, 0, 0, 0, 0, 1},  // 8
  {0, 0, 0, 0, 1, 0, 0, 1}   // 9
};

// Declaración previa de funciones para evitar errores de ámbito
String leerDireccionJoystick();
void mostrarNumero(int n);

void setup() {
  for (int i = pinSegA; i <= pinSegDP; i++) {
    pinMode(i, OUTPUT);
  }
  
  pinMode(pinBuzzer, OUTPUT);
  pinMode(pinJoySW, INPUT_PULLUP);
  
  Serial.begin(115200);
  mostrarNumero(0); 
}

void loop() {
  // 1. Leer la dirección del joystick físico
  String direccion = leerDireccionJoystick();
  
  // 2. Gestionar envío a Java filtrando por tiempo (Debounce)
  static String anterior = "";
  static unsigned long ultimoMovimiento = 0;
  
  if (direccion != "CENTRO") {
    if (direccion != anterior || (millis() - ultimoMovimiento > 250)) {
      Serial.println(direccion);
      anterior = direccion;
      ultimoMovimiento = millis();
      
      if (!muteActivo) {
        tone(pinBuzzer, 800, 40); 
      }
    }
  } else {
    if (anterior != "CENTRO") {
      Serial.println("CENTRO");
      anterior = "CENTRO";
    }
  }
  
  // 3. Escuchar comandos entrantes desde la interfaz de Java
  if (Serial.available() > 0) {
    char comando = Serial.read();
    
    if (comando == 'M') {
      muteActivo = !muteActivo;
      if (muteActivo) noTone(pinBuzzer);
    }
    else if (comando == 'V') {
      while (Serial.available() == 0) {}
      volumen = Serial.read();
    }
    else if (comando >= '0' && comando <= '9') {
      nivelActual = comando - '0';
      mostrarNumero(nivelActual);
      if (!muteActivo) {
        tone(pinBuzzer, 1100, 200); 
      }
    }
  }
  
  delay(10); 
}

// ----- Función para procesar lecturas del Joystick -----
String leerDireccionJoystick() {
  int x = analogRead(pinJoyX);
  int y = analogRead(pinJoyY);
  
  // Zonas ultrasensibles para forzar el movimiento
  if (x < 400) return "IZQUIERDA";
  if (x > 600) return "DERECHA";
  if (y < 400) return "ABAJO";
  if (y > 600) return "ARRIBA";
  
  return "CENTRO";
}

// ----- Función para escribir en el Display de 7 Segmentos -----
void mostrarNumero(int n) {
  if (n < 0 || n > 9) return;
  digitalWrite(pinSegA, numeros[n][0]);
  digitalWrite(pinSegB, numeros[n][1]);
  digitalWrite(pinSegC, numeros[n][2]);
  digitalWrite(pinSegD, numeros[n][3]);
  digitalWrite(pinSegE, numeros[n][4]);
  digitalWrite(pinSegF, numeros[n][5]);
  digitalWrite(pinSegG, numeros[n][6]);
  digitalWrite(pinSegDP, numeros[n][7]);
}