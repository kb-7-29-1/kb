import { defineStore } from 'pinia';

export const useMapStore = defineStore('map', {
  state: () => ({
    filterState: null,
    appliedFilterState: null,
  }),
  getters: {
    hasSavedFilterState: (state) => state.filterState !== null,
  },
  actions: {
    saveFilterState(filterState, appliedFilterState) {
      this.filterState = JSON.parse(JSON.stringify(filterState));
      this.appliedFilterState = JSON.parse(JSON.stringify(appliedFilterState));
    },
    clearFilterState() {
      this.filterState = null;
      this.appliedFilterState = null;
    },
  },
});
