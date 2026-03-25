#!/bin/bash

# Build script for BiteTogether User Service
# This script builds and pushes a multi-platform Docker image WITHOUT GitHub Maven Registry

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="thanhnguyen24/bitetogether-user-service"
TAG="${1:-latest}"

echo -e "${YELLOW}🚀 Building BiteTogether User Service (No GitHub dependency)...${NC}"

# ✅ Ensure buildx is available
echo -e "${YELLOW}🔧 Setting up Docker Buildx...${NC}"
docker buildx create --name multiarch --use --bootstrap 2>/dev/null || docker buildx use multiarch

# ✅ Build & push multi-platform image
echo -e "${YELLOW}📦 Building and pushing image: $IMAGE_NAME:$TAG${NC}"
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t "$IMAGE_NAME:$TAG" \
  --push \
  .

echo -e "${GREEN}✅ Successfully built and pushed $IMAGE_NAME:$TAG${NC}"
echo -e "${GREEN}✅ Image supports both AMD64 and ARM64 architectures${NC}"
echo -e "${YELLOW}💡 Next step: docker pull $IMAGE_NAME:$TAG hoặc docker-compose up -d${NC}"