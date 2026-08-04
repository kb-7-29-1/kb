<script setup>
import api from "@/api/api.js";
import {onMounted, ref} from "vue";

const bookmarks = ref([])

const fetchBookmarks = async () => {
  const response = await api.get('/bookmark')
  bookmarks.value = response.data
}

const removeBookmark = async (propertyId) =>{
  await api.delete(`/bookmark/${propertyId}`)
  await fetchBookmarks();
}

onMounted(fetchBookmarks)
</script>
<template>
  <div class="bg-white rounded-2xl border p-6">
    <h3 class="font-bold mb-4">관심 매물 {{ bookmarks.length }}</h3>

    <div v-if="bookmarks.length === 0" class="text-gray-400 text-sm text-center py-8">
      찜한 매물이 없습니다.
    </div>
    <div v-else class="max-h-96 overflow-y-auto space-y-3 pr-1">
      <div v-for="item in bookmarks" :key="item.propertyId" class="border rounded-xl p-4 flex items-center justify-between">
        ...
      </div>
    </div>
    <div v-for="item in bookmarks" :key="item.propertyId" class="border rounded-xl p-4 mb-3 flex items-center justify-between">
      <div>
        <div class="flex items-center gap-2 mb-1">
          <span class="text-xs bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded">{{ item.buildingTypeTag }}</span>
          <span v-if="item.isIllegalBuilding" class="text-xs">⚠️</span>
        </div>
        <p class="font-semibold">{{ item.address }}</p>
        <p class="text-sm text-gray-500">
          {{ item.deposit }}만 / {{ item.area }}m²
        </p>
      </div>
      <div class="flex items-center gap-2">
        <span class="text-xs bg-orange-100 text-orange-600 px-2 py-1 rounded font-semibold">
          {{ item.safetyScore }}점
        </span>
        <button @click="removeBookmark(item.propertyId)" class="text-red-500">♥</button>
      </div>
    </div>
  </div>
</template>