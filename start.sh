#!/bin/bash
# HeapDump Analyzer v1.0 - Build & Start Script
# Usage:
#   ./start.sh                     # Swing GUI (default)
#   ./start.sh cli <heapfile>      # CLI scan mode
#   ./start.sh build               # Build only

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

JAR_FILE="target/heapdump-analyzer.jar"
JAVA_OPTS="-Xmx4g -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 --enable-native-access=ALL-UNNAMED"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
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

start_swing() {
    build_if_needed
    echo -e "${PURPLE}Starting HeapDump Analyzer (Swing GUI - FlatLaf)...${NC}"
    echo -e "${CYAN}96 spiders | Rule Engine | v1.0${NC}"
    java $JAVA_OPTS -jar "$JAR_FILE" --swing
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

case "${1:-}" in
    cli|--cli|-c)
        start_cli "$2"
        ;;
    build|--build|-b)
        build_if_needed
        echo -e "${GREEN}Build complete: $JAR_FILE${NC}"
        ;;
    help|--help|-h)
        echo "Usage: $0 [mode] [options]"
        echo ""
        echo "Modes:"
        echo "  (no args)       Launch Swing GUI (default)"
        echo "  cli <file>      Launch CLI scan with heap file"
        echo "  build           Build only (compile & package)"
        echo "  help            Show this help"
        ;;
    *)
        start_swing
        ;;
esac
