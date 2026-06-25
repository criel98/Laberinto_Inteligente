package Formulario;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.fazecast.jSerialComm.SerialPort;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;

public class LaberintoDashboard extends JFrame {
    
    private GamePanel panelLaberinto;
    private DashboardPanel panelControl;
    
    private boolean isMuted = false;
    private int sonidoVolumen = 50;
    
    // Variables de comunicación serie
    private SerialPort puertoSerie;
    private OutputStream salidaSerie;
    private InputStream entradaSerie;
    private Scanner scannerSerie;
    private Thread receptorThread;
    
    public LaberintoDashboard() {
        setTitle("Laboratorio Arquitectura de Computadores - Laberinto Inteligente");
        setSize(1020, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);
        
        panelLaberinto = new GamePanel();
        add(panelLaberinto, BorderLayout.CENTER);
        
        panelControl = new DashboardPanel();
        add(panelControl, BorderLayout.EAST);
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                desconectarArduino();
            }
        });
    }
    
    // ----- Conexión Dinámica con Arduino (VERSIÓN DEFINITIVA) -----
    private void conectarArduino(String nombrePuerto) {
        try {
            puertoSerie = SerialPort.getCommPort(nombrePuerto);
            puertoSerie.setComPortParameters(115200, 8, 1, 0);
            
            // Configurar el timeout para que el Scanner escuche correctamente
            puertoSerie.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
            
            if (puertoSerie.openPort()) {
                
                // Pausa obligatoria de 1.5 segundos. Arduino se reinicia al abrir el puerto.
                Thread.sleep(1500); 
                
                salidaSerie = puertoSerie.getOutputStream();
                entradaSerie = puertoSerie.getInputStream();
                scannerSerie = new Scanner(entradaSerie);
                
                receptorThread = new Thread(() -> {
                    while (puertoSerie != null && puertoSerie.isOpen() && scannerSerie != null) {
                        try {
                            if (scannerSerie.hasNextLine()) {
                                String mensaje = scannerSerie.nextLine().trim();
                                if (!mensaje.isEmpty()) {
                                    System.out.println("DATO RECIBIDO DESDE ARDUINO: [" + mensaje + "]");
                                    
                                    SwingUtilities.invokeLater(() -> {
                                        procesarMensajeArduino(mensaje);
                                    });
                                }
                            } else {
                                Thread.sleep(5); 
                            }
                        } catch (Exception e) {
                            break; 
                        }
                    }
                });
                receptorThread.setDaemon(true);
                receptorThread.start();
                
                panelControl.configurarEstadoConexion(true);
                System.out.println("Arduino conectado exitosamente en: " + nombrePuerto);
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo abrir el puerto " + nombrePuerto, "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                panelControl.configurarEstadoConexion(false);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al intentar conectar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            panelControl.configurarEstadoConexion(false);
        }
    }
    
    private void desconectarArduino() {
        try {
            if (puertoSerie != null && puertoSerie.isOpen()) {
                if (receptorThread != null && receptorThread.isAlive()) {
                    receptorThread.interrupt();
                }
                if (scannerSerie != null) scannerSerie.close();
                if (entradaSerie != null) entradaSerie.close();
                if (salidaSerie != null) salidaSerie.close();
                
                puertoSerie.closePort();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            panelControl.configurarEstadoConexion(false);
        }
    }
    
    // ----- Procesar mensajes del Arduino -----
    private void procesarMensajeArduino(String mensaje) {
        switch (mensaje) {
            case "ARRIBA":    panelControl.actualizarJoystick(512, 1023); break;
            case "ABAJO":     panelControl.actualizarJoystick(512, 0);    break;
            case "IZQUIERDA": panelControl.actualizarJoystick(0, 512);    break;
            case "DERECHA":   panelControl.actualizarJoystick(1023, 512); break;
            case "CENTRO":    panelControl.actualizarJoystick(512, 512);  break;
        }
        
        panelLaberinto.moverJugador(mensaje);
    }
    
    private void enviarArduino(char comando) {
        if (puertoSerie != null && puertoSerie.isOpen()) {
            try {
                salidaSerie.write(comando);
                salidaSerie.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // ----- Panel del Laberinto -----
    class GamePanel extends JPanel {
        private static final int TAMANO_CELDA = 40;
        private int[][] laberinto = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1},
            {1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1},
            {1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1},
            {1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1},
            {1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };
        
        private int jugadorX = 1;
        private int jugadorY = 1;
        
        public GamePanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(13 * TAMANO_CELDA, 13 * TAMANO_CELDA));
        }
        
        public void moverJugador(String direccion) {
            int nuevoX = jugadorX;
            int nuevoY = jugadorY;
            
            switch (direccion) {
                case "ARRIBA":    nuevoY--; break;
                case "ABAJO":     nuevoY++; break;
                case "IZQUIERDA": nuevoX--; break;
                case "DERECHA":   nuevoX++; break;
                default: return; 
            }
            
            if (nuevoX >= 0 && nuevoX < 13 && nuevoY >= 0 && nuevoY < 13) {
                if (laberinto[nuevoY][nuevoX] != 1) {
                    jugadorX = nuevoX;
                    jugadorY = nuevoY;
                    repaint(); 
                }
                
                if (laberinto[nuevoY][nuevoX] == 3) {
                    JOptionPane.showMessageDialog(this, "¡GANASTE!", "Victoria", JOptionPane.INFORMATION_MESSAGE);
                    enviarArduino('3'); 
                    jugadorX = 1;
                    jugadorY = 1;
                    repaint();
                }
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int fila = 0; fila < laberinto.length; fila++) {
                for (int col = 0; col < laberinto[fila].length; col++) {
                    int celda = laberinto[fila][col];
                    int x = col * TAMANO_CELDA;
                    int y = fila * TAMANO_CELDA;
                    
                    if (celda == 1) {
                        g.setColor(new Color(80, 80, 80));
                        g.fillRect(x, y, TAMANO_CELDA, TAMANO_CELDA);
                    } else if (celda == 2) {
                        g.setColor(new Color(0, 100, 0));
                        g.fillRect(x, y, TAMANO_CELDA, TAMANO_CELDA);
                    } else if (celda == 3) {
                        g.setColor(Color.YELLOW);
                        g.fillRect(x, y, TAMANO_CELDA, TAMANO_CELDA);
                    }
                }
            }
            g.setColor(Color.GREEN);
            g.fillRect(jugadorX * TAMANO_CELDA + 5, jugadorY * TAMANO_CELDA + 5, TAMANO_CELDA - 10, TAMANO_CELDA - 10);
        }
    }
    
    // ----- Panel de Control -----
    class DashboardPanel extends JPanel {
        private JComboBox<String> comboPuertos;
        private JButton btnConectar;
        private JButton btnMute;
        private JSlider sliderVolumen;
        private VisualizadorJoystick visualizadorJoystick;
        private JLabel lblVolumenValor;
        private JLabel lblInfoConexion;
        
        private boolean conectado = false;
        
        public DashboardPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            setPreferredSize(new Dimension(350, 650));
            setBackground(new Color(30, 30, 40));
            
            JLabel titulo = new JLabel("PANEL DE CONTROL");
            titulo.setFont(new Font("Arial", Font.BOLD, 18));
            titulo.setForeground(Color.WHITE);
            titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(titulo);
            add(Box.createVerticalStrut(15));
            
            JLabel lblSeleccionar = new JLabel("PUERTO ARDUINO:");
            lblSeleccionar.setFont(new Font("Arial", Font.BOLD, 12));
            lblSeleccionar.setForeground(Color.LIGHT_GRAY);
            lblSeleccionar.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(lblSeleccionar);
            add(Box.createVerticalStrut(5));
            
            comboPuertos = new JComboBox<>();
            comboPuertos.setMaximumSize(new Dimension(250, 30));
            comboPuertos.setAlignmentX(Component.CENTER_ALIGNMENT);
            actualizarPuertosDisponibles();
            add(comboPuertos);
            add(Box.createVerticalStrut(10));
            
            btnConectar = new JButton("CONECTAR");
            btnConectar.setFont(new Font("Arial", Font.BOLD, 13));
            btnConectar.setMaximumSize(new Dimension(250, 35));
            btnConectar.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnConectar.setBackground(new Color(0, 150, 255));
            btnConectar.setForeground(Color.WHITE);
            btnConectar.setFocusPainted(false);
            btnConectar.addActionListener(e -> {
                if (!conectado) {
                    if (comboPuertos.getSelectedItem() != null) {
                        conectarArduino(comboPuertos.getSelectedItem().toString());
                    } else {
                        JOptionPane.showMessageDialog(this, "Por favor, selecciona un puerto.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    desconectarArduino();
                }
            });
            add(btnConectar);
            add(Box.createVerticalStrut(15));
            
            JSeparator sep0 = new JSeparator();
            sep0.setMaximumSize(new Dimension(300, 10));
            add(sep0);
            add(Box.createVerticalStrut(10));
            
            JLabel lblJoystick = new JLabel("VISUALIZACIÓN DEL JOYSTICK");
            lblJoystick.setFont(new Font("Arial", Font.BOLD, 13));
            lblJoystick.setForeground(new Color(0, 200, 255));
            lblJoystick.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(lblJoystick);
            add(Box.createVerticalStrut(10));
            
            visualizadorJoystick = new VisualizadorJoystick();
            visualizadorJoystick.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(visualizadorJoystick);
            add(Box.createVerticalStrut(15));
            
            JSeparator sep1 = new JSeparator();
            sep1.setMaximumSize(new Dimension(300, 10));
            add(sep1);
            add(Box.createVerticalStrut(10));
            
            JLabel lblAudio = new JLabel("CONTROL DEL BUZZER");
            lblAudio.setFont(new Font("Arial", Font.BOLD, 13));
            lblAudio.setForeground(new Color(255, 100, 100));
            lblAudio.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(lblAudio);
            add(Box.createVerticalStrut(10));
            
            btnMute = new JButton("MUTE: OFF");
            btnMute.setFont(new Font("Arial", Font.BOLD, 13));
            btnMute.setMaximumSize(new Dimension(200, 35));
            btnMute.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnMute.setBackground(new Color(100, 100, 100));
            btnMute.setForeground(Color.WHITE);
            btnMute.addActionListener(e -> {
                isMuted = !isMuted;
                btnMute.setText(isMuted ? "MUTE: ON" : "MUTE: OFF");
                btnMute.setBackground(isMuted ? new Color(200, 50, 50) : new Color(100, 100, 100));
                enviarArduino('M');
            });
            add(btnMute);
            add(Box.createVerticalStrut(15));
            
            sliderVolumen = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
            sliderVolumen.setMaximumSize(new Dimension(250, 40));
            sliderVolumen.setBackground(new Color(30, 30, 40));
            sliderVolumen.setForeground(Color.WHITE);
            sliderVolumen.addChangeListener(e -> {
                sonidoVolumen = sliderVolumen.getValue();
                lblVolumenValor.setText(sonidoVolumen + "%");
                enviarArduino('V');
                enviarArduino((char)sonidoVolumen);
            });
            add(sliderVolumen);
            
            lblVolumenValor = new JLabel("50%");
            lblVolumenValor.setFont(new Font("Arial", Font.BOLD, 14));
            lblVolumenValor.setForeground(Color.WHITE);
            lblVolumenValor.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(lblVolumenValor);
            add(Box.createVerticalStrut(15));
            
            JSeparator sep2 = new JSeparator();
            sep2.setMaximumSize(new Dimension(300, 10));
            add(sep2);
            add(Box.createVerticalStrut(10));
            
            lblInfoConexion = new JLabel("DESCONECTADO (Modo Simulación)");
            lblInfoConexion.setFont(new Font("Arial", Font.BOLD, 12));
            lblInfoConexion.setForeground(new Color(255, 100, 100));
            lblInfoConexion.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(lblInfoConexion);
        }
        
        private void actualizarPuertosDisponibles() {
            comboPuertos.removeAllItems();
            SerialPort[] puertos = SerialPort.getCommPorts();
            for (SerialPort p : puertos) {
                comboPuertos.addItem(p.getSystemPortName());
            }
        }
        
        public void configurarEstadoConexion(boolean estado) {
            this.conectado = estado;
            if (estado) {
                btnConectar.setText("DESCONECTAR");
                btnConectar.setBackground(new Color(220, 50, 50));
                comboPuertos.setEnabled(false);
                lblInfoConexion.setText("CONECTADO A ARDUINO");
                lblInfoConexion.setForeground(new Color(100, 255, 100));
            } else {
                btnConectar.setText("CONECTAR");
                btnConectar.setBackground(new Color(0, 150, 255));
                comboPuertos.setEnabled(true);
                lblInfoConexion.setText("DESCONECTADO (Modo Simulación)");
                lblInfoConexion.setForeground(new Color(255, 100, 100));
                actualizarPuertosDisponibles();
            }
        }
        
        public void actualizarJoystick(int valorX, int valorY) {
            visualizadorJoystick.setPosicion(valorX, valorY);
            visualizadorJoystick.repaint();
        }
    }
    
    // ----- Visualizador del Joystick -----
    class VisualizadorJoystick extends JPanel {
        private int posX = 512;
        private int posY = 512;
        private static final int RADIO_EXTERNO = 50;
        
        public VisualizadorJoystick() {
            setPreferredSize(new Dimension(140, 140));
            setMaximumSize(new Dimension(140, 140));
            setBackground(Color.DARK_GRAY);
            setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        }
        
        public void setPosicion(int x, int y) {
            this.posX = x;
            this.posY = y;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int centroX = getWidth() / 2;
            int centroY = getHeight() / 2;
            
            int joystickX = centroX + (posX - 512) * RADIO_EXTERNO / 512;
            int joystickY = centroY - (posY - 512) * RADIO_EXTERNO / 512;
            
            g2.setColor(new Color(50, 50, 60));
            g2.fillOval(centroX - RADIO_EXTERNO, centroY - RADIO_EXTERNO, RADIO_EXTERNO * 2, RADIO_EXTERNO * 2);
            
            g2.setColor(new Color(255, 80, 80));
            g2.fillOval(joystickX - 12, joystickY - 12, 24, 24);
            
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.setColor(Color.WHITE);
            g2.drawString("X: " + posX + " Y: " + posY, 5, getHeight() - 5);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LaberintoDashboard().setVisible(true);
        });
    }
}