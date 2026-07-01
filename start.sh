#!/bin/bash
# HeapDump Analyzer v1.0 - Build & Start Script
# Usage:
#   ./start.sh                     # Interactive menu
#   ./start.sh desktop             # JavaFX Desktop GUI
#   ./start.sh web [port]          # Web UI mode
#   ./start.sh cli <heapfile>      # CLI scan mode
#   ./start.sh repl <heapfile>     # Interactive REPL
#   ./start.sh batch <dir>         # Batch scan a directory
#   ./start.sh build               # Build only

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

JAR_FILE="target/heapdump-analyzer.jar"
JAVA_OPTS="-Xmx4g -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 --enable-native-access=ALL-UNNAMED"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

build_if_needed() {
    if [ ! -f "$JAR_FILE" ] || [ "$(find src -newer "$JAR_FILE" -type f 2>/dev/null | head -1)" ]; then
        echo -e "${YELLOW}Building HeapDump Analyzer v1.0...${NC}"
        mvn clean package -q -DskipTests
        if [ $? -ne 0 ]; then
            echo -e "${RED}Build failed!${NC}"
            exit 1
        fi
        echo -e "${GREEN}Build successful!${NC}"
    fi
}

start_desktop() {
    build_if_needed
    # Extract JavaFX mods for module path if not already done
    if [ ! -d "target/javafx-mods" ]; then
        echo -e "${YELLOW}Extracting JavaFX modules...${NC}"
        mkdir -p target/javafx-mods
        JFX_VERSION="21.0.1"
        MAVEN_REPO="$HOME/.m2/repository"
        for mod in javafx-base javafx-controls javafx-fxml javafx-graphics; do
            local jar="$MAVEN_REPO/repository/org/openjfx/$mod/$JFX_VERSION/$mod-$JFX_VERSION.jar"
            if [ -f "$jar" ]; then
                cp "$jar" target/javafx-mods/
            fi
            local plat_jar="$MAVEN_REPO/repository/org/openjfx/$mod/$JFX_VERSION/$mod-$JFX_VERSION-mac-aarch64.jar"
            if [ -f "$plat_jar" ]; then
                cp "$plat_jar" target/javafx-mods/
            else
                plat_jar="$MAVEN_REPO/repository/org/openjfx/$mod/$JFX_VERSION/$mod-$JFX_VERSION-mac.jar"
                if [ -f "$plat_jar" ]; then
                    cp "$plat_jar" target/javafx-mods/
                fi
                plat_jar="$MAVEN_REPO/repository/org/openjfx/$mod/$JFX_VERSION/$mod-$JFX_VERSION-linux.jar"
                if [ -f "$plat_jar" ]; then
                    cp "$plat_jar" target/javafx-mods/
                fi
            fi
        done
    fi

    echo -e "${PURPLE}Starting HeapDump Analyzer (Desktop GUI)...${NC}"
    echo -e "${CYAN}96 spiders | Rule Engine | v1.0${NC}"

    if [ -d "target/javafx-mods" ] && [ "$(ls -1 target/javafx-mods/*.jar 2>/dev/null | wc -l)" -gt 0 ]; then
        java $JAVA_OPTS \
             --module-path target/javafx-mods \
             --add-modules javafx.controls,javafx.fxml \
             -jar "$JAR_FILE" --desktop
    else
        echo -e "${YELLOW}Note: JavaFX modules not found separately. Trying bundled mode...${NC}"
        java $JAVA_OPTS -jar "$JAR_FILE" --desktop
    fi
}

start_cli() {
    build_if_needed
    local heapfile="$1"
    if [ -z "$heapfile" ]; then
        echo -e "${RED}Error: Please specify a heap dump file path${NC}"
        echo "Usage: $0 cli <heapfile> [additional options]"
        exit 1
    fi
    shift
    echo -e "${PURPLE}Starting HeapDump Analyzer (CLI mode)...${NC}"
    java $JAVA_OPTS -jar "$JAR_FILE" "$heapfile" "$@"
}

start_repl() {
    build_if_needed
    local heapfile="$1"
    if [ -z "$heapfile" ]; then
        echo -e "${RED}Error: REPL mode requires a heap dump file path${NC}"
        echo "Usage: $0 repl <heapfile>"
        exit 1
    fi
    echo -e "${PURPLE}Starting HeapDump Analyzer (REPL mode)...${NC}"
    java $JAVA_OPTS -jar "$JAR_FILE" "$heapfile" --repl
}

start_web() {
    build_if_needed
    local port="${1:-9090}"
    echo -e "${PURPLE}Starting HeapDump Analyzer (Web UI)...${NC}"
    echo -e "${CYAN}96 spiders | Rule Engine | v1.0${NC}"
    echo -e "${GREEN}Open http://localhost:${port} in your browser${NC}"
    java $JAVA_OPTS -jar "$JAR_FILE" --web --port "$port"
}

start_batch() {
    build_if_needed
    local dir="$1"
    if [ -z "$dir" ]; then
        echo -e "${RED}Error: Please specify a directory containing heap dump files${NC}"
        echo "Usage: $0 batch <dir> [additional options]"
        exit 1
    fi
    shift
    echo -e "${PURPLE}Starting HeapDump Analyzer (Batch scan)...${NC}"
    java $JAVA_OPTS -jar "$JAR_FILE" --batch "$dir" "$@"
}

show_banner() {
    echo -e "${PURPLE}"
    echo "╔════════════════════════════════════════════════════════╗"
    echo "║         HeapDump Analyzer v1.0                        ║"
    echo "║         Dig secrets out of JVM memory                 ║"
    echo "╚════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

show_menu() {
    show_banner
    echo -e "${CYAN}Select startup mode:${NC}"
    echo ""
    echo -e "  ${GREEN}1)${NC} Desktop GUI     ${YELLOW}(JavaFX desktop application)${NC}"
    echo -e "  ${GREEN}2)${NC} Web UI          ${YELLOW}(Browser-based dashboard)${NC}"
    echo -e "  ${GREEN}3)${NC} CLI Scan        ${YELLOW}(Command-line analysis)${NC}"
    echo -e "  ${GREEN}4)${NC} Batch Scan      ${YELLOW}(Scan a directory of heap dumps)${NC}"
    echo -e "  ${GREEN}5)${NC} REPL            ${YELLOW}(Interactive exploration)${NC}"
    echo -e "  ${GREEN}6)${NC} Build Only      ${YELLOW}(Compile & package)${NC}"
    echo -e "  ${GREEN}7)${NC} Exit"
    echo ""
    read -p "Enter choice [1-7]: " choice

    case $choice in
        1)
            start_desktop
            ;;
        2)
            read -p "Port [9090]: " port
            start_web "${port:-9090}"
            ;;
        3)
            read -p "Heap dump file path: " heapfile
            start_cli "$heapfile"
            ;;
        4)
            read -p "Directory of heap dumps: " dir
            start_batch "$dir"
            ;;
        5)
            read -p "Heap dump file path: " heapfile
            start_repl "$heapfile"
            ;;
        6)
            build_if_needed
            echo -e "${GREEN}Build complete: $JAR_FILE${NC}"
            ;;
        7)
            echo -e "${CYAN}Bye!${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            exit 1
            ;;
    esac
}

# Main entry point
case "${1:-}" in
    desktop|--desktop|gui|--gui|-g)
        start_desktop
        ;;
    web|--web|-w)
        start_web "$2"
        ;;
    cli|--cli|-c)
        start_cli "$2"
        ;;
    batch|--batch)
        start_batch "$2"
        ;;
    repl|--repl|-r)
        start_repl "$2"
        ;;
    build|--build|-b)
        build_if_needed
        echo -e "${GREEN}Build complete: $JAR_FILE${NC}"
        ;;
    help|--help|-h)
        show_banner
        echo "Usage: $0 [mode] [options]"
        echo ""
        echo "Modes:"
        echo "  desktop         Launch JavaFX Desktop GUI"
        echo "  web [port]      Launch Web UI (default port 9090)"
        echo "  cli <file>      Launch CLI scan with heap file"
        echo "  batch <dir>     Batch scan all heap dumps in a directory"
        echo "  repl <file>     Launch interactive REPL with heap file"
        echo "  build           Build only (compile & package)"
        echo "  help            Show this help"
        echo ""
        echo "  (no args)       Show interactive menu"
        ;;
    "")
        show_menu
        ;;
    *)
        # Pass through to CLI for backward compatibility
        build_if_needed
        java $JAVA_OPTS -jar "$JAR_FILE" "$@"
        ;;
esac
