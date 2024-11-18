<template>
  <div class="app-container">
    <!-- NavBar -->
    <NavBar :message=testMe>
      <template #left>
        <router-link to="/">
          <img src="/spysat_logo.png" alt="Logo" class="logo" />
        </router-link>
      </template>

      <template #center>
        <div style="display: flex; gap: 10px; align-items: center;">
          <!-- Filters -->
          <UDropdown v-model="selectedSize.label" :items="sizeItems" mode="hover"
            :popper="{ placement: 'bottom-start' }" style="height: 50px;">
            <UButton color="white" :label="selectedSize?.label || 'SIZE'" />
          </UDropdown>

          <UDropdown v-model="selectedType.label" :items="typeItems" mode="hover"
            :popper="{ placement: 'bottom-start' }" style="height: 50px;">
            <UButton color="white" :label="selectedType?.label || 'TYPE'" />
          </UDropdown>

          <UDropdown v-model="selectedOrbit.label" :items="orbitItems" mode="hover"
            :popper="{ placement: 'bottom-start' }" style="height: 50px;">
            <UButton color="white" :label="selectedOrbit?.label || 'ORBIT'" />
          </UDropdown>

          <UDropdown v-model="selectedCategory.label" :items="categoryItems" mode="hover"
            :popper="{ placement: 'bottom-start' }" style="height: 50px;">
            <UButton color="white" :label="selectedCategory?.label || 'CATEGORY'" />
          </UDropdown>

          <UDropdown v-model="selectedCountry.label" :items="countryItems" mode="hover"
            :popper="{ placement: 'bottom-start' }" style="height: 50px;">
            <UButton color="white" :label="selectedCountry?.label || 'COUNTRY'" />
          </UDropdown>

          <!-- Search Bar -->
          <input v-model="searchQuery" type="text" @input="onInput" placeholder="Search..."
            style="height: 50px; width: 40vw;padding: 10px; font-size: 16px; border: 1px solid #ccc; border-radius: 5px; flex-grow: 1" />
        </div>
      </template>

      <template #right>
        <button @click="logout">Logout</button>
      </template>
    </NavBar>

    <!-- Main content area -->
    <div class="main-content" @click="handleMainClick">
      <!-- <SatelliteDisplay/> -->
      <!-- Canvas  -->
      <div id="canvas-container" ref="canvasContainer" :style="{ width: canvasWidth }">
        <div id="satellitePopup" style="position: absolute; background-color: rgba(0, 0, 0, 0.7); color: white; padding: 5px; border-radius: 5px; display: none;">
  <span id="satelliteName"></span>
</div>
      </div>

      <!-- Slide-over Panel -->
      <div v-show="isPanelOpen" :class="{ open: isPanelOpen }" class="slideover">
        <div class="panel-content">
          <div class="close-bar" @click.stop="togglePanel"
            :style="{ backgroundColor: 'var(--highlight-satelliteGreen)' }">
          </div>
          <div class="panel-details">
          </div>
        </div>
      </div>
    </div>
  </div>
</template>


<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import NavBar from '@@/component/NavBar.vue'
// import { updateSatelliteDisplay, satellitesData, initThree } from '@@/component/SatelliteDisplay.vue'
// import { useSatelliteDisplay } from '../component/Display.js'
// const { updateSatelliteDisplay, satellitesData, resizeCanvas, } = useSatelliteDisplay()

import { text } from '../component/State.js'
import { auth } from '@/composables/auth'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import getStarfield from '~/composables/starfield'
import axios from 'axios';
import { sgp4, twoline2satrec, propagate, gstime, eciToGeodetic } from 'satellite.js';
// var SATELLITE = require('satellite.js');

const ORBITAL_LEVELS = ["LEO", "MEO", "HEO", "GEO", "OTHER"];
const SATELLITE_SIZE = ["SMALL", "MEDIUM", "LARGE"];
const SATELLITE_TYPE = ["SATELLITE", "ROCKET BODY", "DEBRIS", "UNKNOWN"];
const DISCIPLINES = ["ASTRONOMY", "EARTH SCIENCE", "PLANETARY SCIENCE", "SOLAR PHYSICS", "SPACE PHYSICS", "LIFE SCIENCE", "MICROGRAVITY", "HUMAN CREW", "ENGINEERING", "COMMUNICATIONS", "NAVIGATION/GLOBAL POSITIONING", "SURVEILLANCE AND OTHER MILITARY", "RESUPPLY/REFURBISHMENT/REPAIR", "TECHNOLOGY APPLICATIONS", "UNCATEGORIZED COSMOS", "OTHER"];
const COUNTRIES = [
  "ALGERIA", "ARAB SATELLITE COMMUNICATIONS ORGANIZATION", "ARGENTINA", "ASIA SATELLITE TELECOMMUNICATIONS COMPANY (ASIASAT)",
  "AUSTRALIA", "AZERBAIJAN", "BANGLADESH", "BELARUS", "BOLIVIA", "BRAZIL", "BULGARIA", "CANADA", "CHILE", "CHINA/BRAZIL",
  "COMMONWEALTH OF INDEPENDENT STATES (FORMER USSR)", "CZECH REPUBLIC (FORMER CZECHOSLOVAKIA)", "CZECHIA", "DENMARK",
  "ECUADOR", "EGYPT", "ESTONIA", "EUROPEAN ORGANISATION FOR THE EXPLOITATION OF METEOROLOGICAL SATELLITES",
  "EUROPEAN SPACE AGENCY", "EUROPEAN TELECOMMUNICATIONS SATELLITE ORGANIZATION", "FRANCE", "FRANCE/GERMANY",
  "FRANCE/ITALY", "GERMANY", "GLOBALSTAR", "GREECE", "HUNGARY", "INDIA", "INDONESIA", "INTERNATIONAL MOBILE SATELLITE ORGANIZATION (INMARSAT)",
  "INTERNATIONAL SPACE STATION", "INTERNATIONAL TELECOMMUNICATIONS SATELLITE ORGANIZATION", "IRAN", "IRAQ", "ISRAEL", "ITALY",
  "JAPAN", "KAZAKHSTAN", "KENYA", "KUWAIT", "LAOS", "LITHUANIA", "LUXEMBOURG", "MALAYSIA", "MEXICO", "MOROCCO", "NETHERLANDS", "NEW ICO",
  "NEW ZEALAND", "NIGERIA", "NORTH ATLANTIC TREATY ORGANIZATION", "NORTH KOREA", "NORWAY", "O3B NETWORKS",
  "ORBCOMM", "PAKISTAN", "PEOPLE'S REPUBLIC OF CHINA", "PERU", "PHILIPPINES (REPUBLIC OF THE PHILIPPINES)", "POLAND",
  "PORTUGAL", "QATAR", "REGIONAL AFRICAN SATELLITE COMMUNICATIONS ORGANIZATION", "REPUBLIC OF RWANDA",
  "REPUBLIC OF SLOVENIA", "REPUBLIC OF TUNISIA", "SAUDI ARABIA", "SEA LAUNCH", "SINGAPORE", "SINGAPORE/TAIWAN",
  "SOCIETE EUROPEENNE DES SATELLITES", "SOUTH AFRICA", "SOUTH KOREA", "SPAIN", "SWEDEN", "TAIWAN (REPUBLIC OF CHINA)", "THAILAND",
  "TURKEY", "TURKMENISTAN/MONACO", "UNITED ARAB EMIRATES", "UNITED KINGDOM", "UNITED STATES", "UNITED STATES/BRAZIL",
  "URUGUAY", "VENEZUELA", "VIETNAM"]

const testMessage = "Test"

const { logout } = auth()
const isPanelOpen = ref(true)
const canvasContainer = ref(null)
const isDropdownOpen = ref(false)

const selectedSize = ref({ label: 'SIZE' });
const selectedType = ref({ label: 'TYPE' });
const selectedOrbit = ref({ label: 'ORBIT' });
const selectedCategory = ref({ label: 'CATEGORY' });
const selectedCountry = ref({ label: 'COUNTRY' });
const searchQuery = ref("")
const debounceTimeout = ref(null)

let currentSatellites = ref(null)
const SCALE_FACTOR = 1 / 6371;
let hoveredInstanceId = -1;
let hoveredInstanceOriginalColor = new THREE.Color();
const hoverColor = new THREE.Color(0xffffff);
const selectedColor = new THREE.Color(0x00dfff);
let selectedInstanceId = -1;
let selectedInstanceOriginalColor = new THREE.Color();

const satelliteGreen = new THREE.Color(0x7ed957);
const rocketOrange = new THREE.Color(0xff914d);
const debrisRed = new THREE.Color(0xff3131);
const unknownYellow = new THREE.Color(0xffde59);

const toast = useToast()

const togglePanel = () => {
  isPanelOpen.value = !isPanelOpen.value
  resizeCanvas()
}

const canvasWidth = computed(() => (isPanelOpen.value ? '100vw' : '77vw'))

const handleMainClick = (event) => {
  if (!isPanelOpen.value && event.clientX > window.innerWidth * 0.66) {
    togglePanel()
  }
}

let scene, camera, renderer, globe, raycaster, pointer, mesh

const initThree = () => {
  if (!canvasContainer.value) return

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(
    75,
    canvasContainer.value.offsetWidth / canvasContainer.value.offsetHeight,
    0.1,
    1000
  )
  camera.position.z = 5

  raycaster = new THREE.Raycaster();
  pointer = new THREE.Vector2();

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(canvasContainer.value.offsetWidth, canvasContainer.value.offsetHeight)

  canvasContainer.value.appendChild(renderer.domElement)

  const earthGroup = new THREE.Group();
  earthGroup.rotation.z = -23.4 * Math.PI / 180

  scene.add(earthGroup)

  const light = new THREE.HemisphereLight(0xffffbb, 0x080820, 10);
  scene.add(light);

  const controls = new OrbitControls(camera, renderer.domElement);
  controls.minDistance = 2;
  controls.maxDistance = 30;
  controls.enablePan = false;
  controls.target.set(0, 0, 0);
  controls.update();

  const texture = new THREE.TextureLoader().load('/earthmap.jpg');
  texture.colorSpace = THREE.SRGBColorSpace;
  const geometry = new THREE.IcosahedronGeometry(1, 12)
  const material = new THREE.MeshBasicMaterial({
    map: texture,
    transparent: true,
    side: THREE.DoubleSide,
  })


  globe = new THREE.Mesh(geometry, material)
  scene.add(globe)

  const stars = getStarfield(1000)
  scene.add(stars)

  renderer.setAnimationLoop(animate)
}

const updateSatelliteDisplay = () => {
  scene.remove(mesh)
  const satelliteBody = new THREE.IcosahedronGeometry(0.023, 1);
  const satelliteMaterial = new THREE.MeshBasicMaterial();
  mesh = new THREE.InstancedMesh(satelliteBody, satelliteMaterial, currentSatellites.value.length);

  const satellite = new THREE.Object3D();

  for (let i = 0; i < currentSatellites.value.length; i++) {
    let sat = currentSatellites.value[i];
    let tle_Line1 = sat['TLE_LINE1'];
    let tle_Line2 = sat['TLE_LINE2'];
    let satrec = twoline2satrec(tle_Line1, tle_Line2);

    const positionAndVelocity = propagate(satrec, new Date());
    const positionEci = positionAndVelocity.position;

    if (positionEci) {

      const gmst = gstime(new Date());
      const positionGd = eciToGeodetic(positionEci, gmst);

      const radius = (6371 + positionGd.height) * SCALE_FACTOR;
      const x = radius * Math.cos(positionGd.latitude) * Math.cos(positionGd.longitude);
      const z = radius * Math.cos(positionGd.latitude) * Math.sin(positionGd.longitude);
      const y = radius * Math.sin(positionGd.latitude);

      satellite.position.set(x, y, z);
      satellite.updateMatrix();

      mesh.userData[i] = sat

      mesh.setMatrixAt(i, satellite.matrix);

      let color;
      if (sat.OBJECT_TYPE === "PAYLOAD") color = satelliteGreen;
      else if (sat.OBJECT_TYPE === "ROCKET BODY") color = rocketOrange;
      else if (sat.OBJECT_TYPE === "DEBRIS") color = debrisRed;
      else color = unknownYellow;
      mesh.setColorAt(i, color);
    }
  }
  mesh.instanceColor.needsUpdate = true;
  scene.add(mesh);
}

// window.addEventListener('pointerdown', onPointerDown);

function onPointerDown(event) {
  if (hoveredInstanceId !== -1) {
    // Reset the previously selected satellite's color (if different from the new one)
    console.log(`Clicked on satellite instance ${selectedInstanceId}`);
    if (selectedInstanceId !== -1 && selectedInstanceId !== hoveredInstanceId) {
      let previousSatellite = mesh.userData[selectedInstanceId];
      let previousColor;

      // Determine the type color for the previous selection
      if (previousSatellite.OBJECT_TYPE === "PAYLOAD") previousColor = satelliteGreen;
      else if (previousSatellite.OBJECT_TYPE === "ROCKET BODY") previousColor = rocketOrange;
      else if (previousSatellite.OBJECT_TYPE === "DEBRIS") previousColor = debrisRed;
      else previousColor = unknownYellow;

      mesh.setColorAt(selectedInstanceId, previousColor);
    }

    // Set the clicked satellite as the selected one
    selectedInstanceId = hoveredInstanceId;
    console.log(`Clicked on satellite instance ${selectedInstanceId}`);   
    mesh.setColorAt(selectedInstanceId, selectedColor);
    mesh.instanceColor.needsUpdate = true;

    
    console.log(mesh.userData[selectedInstanceId]);
  }
}

function onMouseMove(event) {
  const canvas = renderer.domElement;
  const rect = canvas.getBoundingClientRect();
  pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
  pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

  raycaster.setFromCamera(pointer, camera);
  const popup = document.getElementById('satellitePopup');
  const satelliteNameElement = document.getElementById('satelliteName');

  const intersects = raycaster.intersectObject(mesh, true); // Ensure "true" for child objects, if any
  if (intersects.length > 0) {
    const instanceId = intersects[0].instanceId; // Closest instance

    if (instanceId !== hoveredInstanceId) {
      // Reset previous hover (if it's not the selected one)
      if (hoveredInstanceId !== -1 && hoveredInstanceId !== selectedInstanceId) {
        let previousSatellite = mesh.userData[hoveredInstanceId];
        let previousColor;

        // Determine the type color
        if (previousSatellite.OBJECT_TYPE === "PAYLOAD") previousColor = satelliteGreen;
        else if (previousSatellite.OBJECT_TYPE === "ROCKET BODY") previousColor = rocketOrange;
        else if (previousSatellite.OBJECT_TYPE === "DEBRIS") previousColor = debrisRed;
        else previousColor = unknownYellow;

        mesh.setColorAt(hoveredInstanceId, previousColor);
        mesh.instanceColor.needsUpdate = true;
      }

      // Highlight new hover (if it's not the selected one)
      if (instanceId !== selectedInstanceId) {
        hoveredInstanceId = instanceId;
        mesh.setColorAt(instanceId, hoverColor);
        mesh.instanceColor.needsUpdate = true;

        let satellite = mesh.userData[instanceId];
        satelliteNameElement.textContent = satellite.OBJECT_NAME;  // Set the satellite name
        popup.style.display = 'block';  // Show the popup
        popup.style.left = `${event.clientX + 15}px`;  // Adjust X position to avoid overlap with cursor
        popup.style.top = `${event.clientY - 65}px`;
      }
    }
  } else {
    // Reset hover when nothing is intersected (if it's not the selected one)
    if (hoveredInstanceId !== -1 && hoveredInstanceId !== selectedInstanceId) {
      let previousSatellite = mesh.userData[hoveredInstanceId];
      let previousColor;

      // Determine the type color
      if (previousSatellite.OBJECT_TYPE === "PAYLOAD") previousColor = satelliteGreen;
      else if (previousSatellite.OBJECT_TYPE === "ROCKET BODY") previousColor = rocketOrange;
      else if (previousSatellite.OBJECT_TYPE === "DEBRIS") previousColor = debrisRed;
      else previousColor = unknownYellow;

      mesh.setColorAt(hoveredInstanceId, previousColor);
      mesh.instanceColor.needsUpdate = true;
      hoveredInstanceId = -1;

      popup.style.display = 'none';
    }
  }
}



const animate = () => {
  // globe.rotation.y += 0.0001
  renderer.render(scene, camera)
}

const resizeCanvas = () => {
  if (renderer && camera && canvasContainer.value) {
    const width = canvasContainer.value.offsetWidth
    const height = canvasContainer.value.offsetHeight

    renderer.setSize(width, height)


    camera.aspect = width / height
    camera.updateProjectionMatrix()
  }
}

const handleKeydown = (event) => {
  if (event.key === 'Enter') {
    togglePanel()
  }
}

const getAllSatellites = async () => {
  try {
    const response = await axios.get(`http://localhost:8080/satellite/filters`);
    currentSatellites.value = response.data;
    updateSatelliteDisplay()
  } catch (error) {
    console.error("Error fetching satellites:", error);
  }
}


onMounted(() => {
  welcomeToSpysat()
  if (canvasContainer.value) {
    initThree()
    getAllSatellites()
    window.addEventListener('resize', resizeCanvas)
    window.addEventListener('keydown', handleKeydown)
    window.addEventListener('pointerdown', onPointerDown)
    window.addEventListener('mousemove', onMouseMove);
    if (renderer && camera && canvasContainer.value) {
      const width = 1283
      const height = canvasContainer.value.offsetHeight
      renderer.setSize(width, height)


      camera.aspect = width / height
      camera.updateProjectionMatrix()
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCanvas)
  window.removeEventListener('keydown', handleKeydown)

  if (renderer) {
    renderer.dispose()
    renderer.forceContextLoss()
  }
})

watch(
  [selectedSize, selectedType, selectedOrbit, selectedCategory, selectedCountry],
  () => {
    if (
      selectedSize.value.label !== 'SIZE' ||
      selectedType.value.label !== 'TYPE' ||
      selectedOrbit.value.label !== 'ORBIT' ||
      selectedCategory.value.label !== 'CATEGORY' ||
      selectedCountry.value.label !== 'COUNTRY'
    ) {
      searchQuery.value = '';
    }
  }
);

const applyFilter = async () => {
  const filters = {
    size: selectedSize.value.label !== 'SIZE' ? selectedSize.value.label + "_SIZE" : undefined,
    type: selectedType.value.label !== 'TYPE' ? selectedType.value.label === "SATELLITE" ? "PAYLOAD" : selectedType.value.label  : undefined,
    level: selectedOrbit.value.label !== 'ORBIT' ? selectedOrbit.value.label === "OTHER" ? "OTHER_ORBIT" : selectedOrbit.value.label : undefined,
    discipline: selectedCategory.value.label !== 'CATEGORY' ? selectedCategory.value.label : undefined,
    country: selectedCountry.value.label !== 'COUNTRY' ? selectedCountry.value.label : undefined
  };

  const queryParams = Object.entries(filters)
    .filter(([_, value]) => value !== undefined)
    .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
    .join('&');

  const url = `http://localhost:8080/satellite/filters?${queryParams}`;

  try {
    const { data } = await axios.get(url);
    currentSatellites.value = data;
    if(currentSatellites.value.length == 0){
        onSearchNone()
    }
    updateSatelliteDisplay()
    hoveredInstanceId = -1;
    selectedInstanceId = -1
  } catch (error) {
    console.error('Error fetching filtered satellites:', error);
  }
}

const generateFilterItems = (options, selected) => {
  return options.map(option => [{
    label: option,
    click: () => {
      selected.value = selected.value.label === option ? { label: selected.label } : { label: option };
      applyFilter();
    }
  }]);
};

const performSearch = async () => {
  if (searchQuery) {
    try {
      const response = await axios.post(`http://localhost:8080/satellite/search` + "?searchQuery=" + searchQuery.value);
      
      currentSatellites.value = response.data
      if(currentSatellites.value.length == 0){
        onSearchNone()
      }
      updateSatelliteDisplay()
      hoveredInstanceId = -1;
      selectedInstanceId = -1
    } catch (error) {
      console.error("Error during search:", error)
    }
  }
}

const onInput = () => {

  resetFilters()
  text.value = searchQuery.value
  if (debounceTimeout.value) clearTimeout(debounceTimeout.value);

  debounceTimeout.value = setTimeout(() => {
    if (searchQuery.value.trim() !== '') {
      performSearch()
    }
  }, 1000);
};

function resetFilters() {
  selectedSize.value = { label: 'SIZE' };
  selectedType.value = { label: 'TYPE' };
  selectedOrbit.value = { label: 'ORBIT' };
  selectedCategory.value = { label: 'CATEGORY' };
  selectedCountry.value = { label: 'COUNTRY' };
}

const onSearchNone = () => {
  toast.add({
    title: 'No Satellites found',
    description: 'Please try a different search for success',
     color: 'blue',
     ui: {
     wrapper: 'fixed bottom-4 left-4 w-full max-w-sm', // Set max width to a larger value (max-w-sm or max-w-md)
    container: 'bg-[color:var(--primary-50)] text-white rounded-lg shadow-lg'
  },
  })
}

const onFilterNone = () => {
  toast.add({
    title: 'No Satellites found',
    description: 'Please try a different filter for success',
     color: 'blue',
     ui: {
     wrapper: 'fixed bottom-4 left-4 w-full max-w-sm', // Set max width to a larger value (max-w-sm or max-w-md)
    container: 'bg-[color:var(--primary-50)] text-white rounded-lg shadow-lg'
  },
  })
}

const sizeItems = generateFilterItems(SATELLITE_SIZE, selectedSize);
const typeItems = generateFilterItems(SATELLITE_TYPE, selectedType);
const orbitItems = generateFilterItems(ORBITAL_LEVELS, selectedOrbit);
const categoryItems = generateFilterItems(DISCIPLINES, selectedCategory);
const countryItems = generateFilterItems(COUNTRIES, selectedCountry);

const welcomeToSpysat = () => {
  const panelDetails = document.querySelector('.panel-details');

  panelDetails.innerHTML = "<h2>Welcome to SpySat</h2>"
  panelDetails.innerHTML += "<br>"
  panelDetails.innerHTML += "<p>This website is a satellite visualization tool. It has live tracked data and plenty of information for you to explore. Displayed around the globe you will find satellites, debris, rocket bodies, and any unknown objects orbiting our Earth.</p>"
  panelDetails.innerHTML += "<br>"
  panelDetails.innerHTML += "<p>Use filters to find all satellites that match your preferences to narrow the globe display</p>"
  panelDetails.innerHTML += "<br>"
  panelDetails.innerHTML += "<p>Use the search bar to find all satellites whose names match or find a specific satellite by its ID</p>"
  panelDetails.innerHTML += "<br>"
  panelDetails.innerHTML += "<p>Use these to interact and explore our space above</p>"
  panelDetails.innerHTML += "<p>Select: Left click</p>"
  panelDetails.innerHTML += "<p>Zoom: Scroll in </p>"
  panelDetails.innerHTML += "<p>Rotate: Left click and drag</p>"
  panelDetails.innerHTML += "<p>Enter: Close the side panel for better viewing</p>"
}



</script>





<style scoped>
/* App Container (Main Wrapper) */
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

/* Main content area */
.main-content {
  display: flex;
  flex-grow: 1;
  position: relative;
  height: calc(100vh - 4.5rem);
  transition: margin-right 0.75s ease-in-out;
}

#canvas-container {
  height: 100%;
  transition: width 0.75s ease-in-out;
}

/* Slide panel */
.slideover {
  position: fixed;
  top: 4.5vw;
  right: 0;
  width: 23vw;
  height: 100vh;
  background-color: var(--primary-100);
  display: flex;
  flex-direction: row;
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.2);
  transform: translateX(100%);
  transition: transform 0.75s ease-in-out;
  z-index: 10;
}

.slideover.open {
  transform: translateX(0);
}

.panel-content {
  display: flex;
  flex-direction: row;
  height: 100%;
}

.close-bar {
  width: 0.5rem;
  height: 100%;
  background-color: var(--highlight-satelliteGreen);
  cursor: pointer;
}

.panel-details {
  padding: 1rem;
  flex: 1;
  color: var(--primary-500);
}

.logo {
  width: 50px;
  height: auto;
  max-width: 100%;
  padding: 5px;
}

.nav-items>* {
  margin-right: 10px;
}

.large-dropdown {
  width: 220px;
}

.large-button {
  width: 180px;
  height: 45px;
  font-size: 1.2rem;
  /* Adjust the font size */
  padding: 12px;
  /* Adjust padding */
}

.dropdown-menu {
  width: 220px;
  /* Match with dropdown width */
}

.dropdown-item {
  padding: 12px 20px;
  /* Increase padding */
  font-size: 1.5rem;
  /* Adjust font size */
}
</style>
