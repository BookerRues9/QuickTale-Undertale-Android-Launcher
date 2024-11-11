#include <jni.h>
#include <android/log.h>
#include <android/input.h>
#include <android/native_activity.h>
// prueba de controles tactiles incompleta xd 
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "TobyFoxEmulator", __VA_ARGS__))

// Enum de teclas
enum Teclas {
    KEY_UP = 0,
    KEY_DOWN,
    KEY_LEFT,
    KEY_RIGHT,
    KEY_Z,
    KEY_X,
    KEY_C,
    KEY_ESC,
    NUM_KEYS
};

// Para almacenar el estado de las teclas (presionada o no)
bool teclasVirtuales[NUM_KEYS] = {false};

// Función para simular el KeyEvent de una tecla
void simularTecla(int tecla, bool presionada) {
    teclasVirtuales[tecla] = presionada;
    LOGI("Simulando tecla: %d, presionada: %d", tecla, presionada);
    // Aquí puedes enviar un evento de teclado utilizando un InputEvent o manipular la lógica del juego
}

// Función para manejar eventos táctiles
void manejarEventoDeTacto(AInputEvent *event) {
    int action = AMotionEvent_getAction(event);  // Acción (presionado o liberado)
    int pointerCount = AMotionEvent_getPointerCount(event);  // Número de toques

    // Recorremos cada dedo tocando la pantalla
    for (int i = 0; i < pointerCount; i++) {
        float x = AMotionEvent_getX(event, i);
        float y = AMotionEvent_getY(event, i);

        // Asignamos áreas de la pantalla a teclas (flechas y ZXC)
        if (x < 100 && y < 100) {  // Zona de flecha arriba
            simularTecla(KEY_UP, action == AKEY_EVENT_ACTION_DOWN);
        }
        if (x < 100 && y > 300) {  // Zona de flecha abajo
            simularTecla(KEY_DOWN, action == AKEY_EVENT_ACTION_DOWN);
        }
        if (x < 100) {  // Zona de flecha izquierda
            simularTecla(KEY_LEFT, action == AKEY_EVENT_ACTION_DOWN);
        }
        if (x > 300) {  // Zona de flecha derecha
            simularTecla(KEY_RIGHT, action == AKEY_EVENT_ACTION_DOWN);
        }
        if (y < 200 && x > 200) {  // Zona de tecla Z
            simularTecla(KEY_Z, action == AKEY_EVENT_ACTION_DOWN);
        }
        if (y < 200 && x > 300) {  // Zona de tecla X
            simularTecla(KEY_X, action == AKEY_EVENT_ACTION_DOWN);
        }
        if (y < 200 && x > 400) {  // Zona de tecla C
            simularTecla(KEY_C, action == AKEY_EVENT_ACTION_DOWN);
        }
        if (y > 500 && x > 400) {  // Zona de ESC
            simularTecla(KEY_ESC, action == AKEY_EVENT_ACTION_DOWN);
        }
    }
}

// Función para procesar la entrada de eventos táctiles
int procesarEntrada(AInputEvent *event) {
    if (AInputEvent_getType(event) == AINPUT_EVENT_TYPE_MOTION) {
        manejarEventoDeTacto(event);  // Llamar a la función de manejo de eventos
    }
    return 1;
}

