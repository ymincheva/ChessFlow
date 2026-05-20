#include <cstring>
#include <iostream>
#include <jni.h>
#include <sstream>
#include <string>
#include <thread>
#include <unistd.h>
#include <vector>

#include "stockfish/bitboard.h"
#include "stockfish/misc.h"
#include "stockfish/position.h"
#include "stockfish/tune.h"
#include "stockfish/types.h"
#include "stockfish/uci.h"
#include <fcntl.h>

int input_pipe[2];
int output_pipe[2];

FILE *stockfish_out = nullptr;
FILE *stockfish_in = nullptr;

void stockfish_main() {
    using namespace Stockfish;

    const int argc = 1;
    const char *argv[] = {"stockfish"};

    try {
        Bitboards::init();
        Position::init();

        UCIEngine uci(argc, const_cast<char **>(argv));
        Tune::init(uci.engine_options());

        uci.loop();
    } catch (...) {
        std::cout << "info string Engine exited." << std::endl;
        std::cout.flush();
    }
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_chessflow_jni_StockfishEngine_initializeEngine(JNIEnv *env, jobject thiz) {
    if (pipe(input_pipe) < 0 || pipe(output_pipe) < 0) {
        return;
    }

    dup2(input_pipe[0], STDIN_FILENO);
    dup2(output_pipe[1], STDOUT_FILENO);

    stockfish_in = fdopen(input_pipe[1], "w");
    stockfish_out = fdopen(output_pipe[0], "r");

    int flags = fcntl(output_pipe[0], F_GETFL, 0);
    fcntl(output_pipe[0], F_SETFL, flags | O_NONBLOCK);

    std::thread([]() {
        stockfish_main();
    }).detach();
}

JNIEXPORT jstring JNICALL
Java_com_chessflow_jni_StockfishEngine_readOutput(JNIEnv *env, jobject) {
    char line[2048];
    if (stockfish_out && fgets(line, sizeof(line), stockfish_out)) {
        return env->NewStringUTF(line);
    }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_chessflow_jni_StockfishEngine_sendCommand(JNIEnv *env, jobject, jstring jcmd) {
    const char *cmd = env->GetStringUTFChars(jcmd, nullptr);

    if (stockfish_in != nullptr) {
        if (fprintf(stockfish_in, "%s\n", cmd) < 0) {
            stockfish_in = nullptr;
        } else {
            fflush(stockfish_in);
        }
    } else {
    }

    env->ReleaseStringUTFChars(jcmd, cmd);
}

extern "C" JNIEXPORT void JNICALL
Java_com_chessflow_jni_StockfishEngine_shutdownEngine(JNIEnv *env, jobject thiz) {
    if (stockfish_in != nullptr) {
        fclose(stockfish_in);
        stockfish_in = nullptr;
    }

    if (stockfish_out != nullptr) {
        fclose(stockfish_out);
        stockfish_out = nullptr;
    }

    close(input_pipe[0]);
    close(input_pipe[1]);
    close(output_pipe[0]);
    close(output_pipe[1]);
}
}