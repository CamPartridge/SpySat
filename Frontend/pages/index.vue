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
          <UDropdown class="custom-dropdown" v-model="selectedSize.label" :items="sizeItems" mode="hover"
            :popper="{ placement: 'bottom-start' }" style="height: 50px;">
            <UButton color="white" :label="selectedSize?.label || 'SIZE'" class="custom-button" />
          </UDropdown>

          <UDropdown class="custom-dropdown" v-model="selectedType.label" :items="typeItems" mode="hover"
            :popper="{ placement: 'bottom-start' }" style="height: 50px;">
            <UButton color="white" :label="selectedType?.label || 'TYPE'" class="custom-button" />
          </UDropdown>

          <UDropdown class="custom-dropdown" v-model="selectedOrbit.label" :items="orbitItems" mode="hover"
            :popper="{ placement: 'bottom-start' }" style="height: 50px;">
            <UButton color="white" :label="selectedOrbit?.label || 'ORBIT'" class="custom-button" />
          </UDropdown>

          <UDropdown class="custom-dropdown" v-model="selectedCategory.label" :items="categoryItems" mode="hover"
            :popper="{ placement: 'bottom-start' }" style="height: 50px;">
            <UButton color="white" :label="selectedCategory?.label || 'CATEGORY'" class="custom-button" />
          </UDropdown>


          <UDropdown class="custom-dropdown" v-model="selectedCountry.label" :items="countryItems" mode="hover" :popper="{
            placement: 'bottom-start'}" style="height: 50px;">
            <UButton color="white" :label="selectedCountry?.label || 'COUNTRY'" class="custom-button" />
          </UDropdown>

          <!-- Search Bar -->
          <input v-model="searchQuery" type="text" @input="onInput" placeholder="Search..."
            style="font-size: 22px; color: #00ffff ; height: 50px; width: 40vw;padding: 10px; border: 1px solid #ccc; border-radius: 5px; flex-grow: 1" />
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
        <div id="satellitePopup"
          style="position: absolute; background-color: rgba(0, 0, 0, 0.7); color: white; padding: 5px; border-radius: 5px; display: none;">
          <span id="satelliteName"></span>
        </div>
      </div>

      <!-- Slide-over Panel -->
      <div v-show="isPanelOpen" :class="{ open: isPanelOpen }" class="slideover">
        <div class="close-bar" @click.stop="togglePanel" :style="{ backgroundColor: 'var(--interaction)' }">
        </div>
        <div class="panel-content">
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
import { _fontSize } from '#tailwind-config/theme'
const { $dropdownConfig } = useNuxtApp();
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

const COUNTRY_CODES = [
  "ALG", "AB", "ARGN", "AC", "AUS", "AZER", "BGD", "BELA", "BOL",
  "BRAZ", "BGR", "CA", "CHLE", "CHBZ", "CIS", "CZCH", "CZ", "DEN",
  "ECU", "EGYP", "EST", "EUME", "ESA", "EUTE", "FR", "FGER", "FRIT",
  "GER", "GLOB", "GREC", "HUN", "IND", "INDO", "IM", "ISS", "ITSO", "IRAN",
  "IRAK", "ISRA", "IT", "JPN", "KAZ", "KEN", "KWT", "LAOS", "LTU", "LUXE",
  "MALA", "MEX", "MA", "NETH", "NICO", "NZ", "NIG", "NATO", "NKOR", "NOR",
  "O3B", "ORB", "PAKI", "PRC", "PER", "RP", "POL", "POR", "QAT", "RASC",
  "RWA", "SVN", "TUN", "SAUD", "SEAL", "SING", "STCT", "SES", "SAFR",
  "SKOR", "SPN", "SWED", "ROC", "THAI", "TURK", "TMMC", "UAE", "UK",
  "US", "USBZ", "URY", "VENZ", "VTNM"
];

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
  if (isPanelOpen) {
    const closeBar = document.querySelector(".close-bar")

    closeBar.style.backgroundColor = 'var(--interaction)'

    welcomeToSpysat()
  }
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
function onPointerDown(event) {
  if (hoveredInstanceId !== -1) {
    // Reset the previously selected satellite's color (if different from the new one)
    console.log(`Clicked on satellite instance ${selectedInstanceId}`);
    if (selectedInstanceId !== -1 && selectedInstanceId !== hoveredInstanceId) {
      const popup = document.getElementById('satellitePopup');
      popup.style.display = 'none';


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
    satellitePanelDisplay(mesh.userData[selectedInstanceId])
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

  updateRedis()

})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCanvas)
  window.removeEventListener('keydown', handleKeydown)

  if (renderer) {
    renderer.dispose()
    renderer.forceContextLoss()
  }
})

const updateRedis = async () => {
  axios.post("http://localhost:8080/satellite/filters")
}

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
    type: selectedType.value.label !== 'TYPE' ? selectedType.value.label === "SATELLITE" ? "PAYLOAD" : selectedType.value.label : undefined,
    level: selectedOrbit.value.label !== 'ORBIT' ? selectedOrbit.value.label === "OTHER" ? "OTHER_ORBIT" : selectedOrbit.value.label : undefined,
    discipline: selectedCategory.value.label !== 'CATEGORY' ? selectedCategory.value.label : undefined,
    country: selectedCountry.value.label !== 'COUNTRY' ? selectedCountry.value.label : undefined
  };

  const queryParams = Object.entries(filters)
    .filter(([_, value]) => value !== undefined && value !== "undefined_SIZE")
    .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
    .join('&');

  const url = queryParams
    ? `http://localhost:8080/satellite/filters?${queryParams}` // If filters are applied
    : `http://localhost:8080/satellite/filters`;

  try {
    const { data } = await axios.get(url);
    currentSatellites.value = data;
    if (currentSatellites.value.length == 0) {
      onFilterNone()
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
    class: "group flex items-center gap-1. w-full px-1.5 py-1.5 rounded-md text-xl", // Dropdown item base classes
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
      if (currentSatellites.value.length == 0) {
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
const appConfig = useAppConfig();
console.log(appConfig.ui);

const onSearchNone = () => {
  toast.add({
    title: 'No Satellites found',
    description: 'Please try a different search for success',
    color: 'red',
    ui: {
      ...appConfig.ui.toast.slots,
      wrapper: 'fixed bottom-4 left-4 w-full max-w-sm',
      container: 'bg-[color:var(--primary-50)] rounded-lg shadow-lg',
    },
  })
}

const onFilterNone = () => {
  toast.add({
    title: 'No Satellites found',
    description: ' Please try a different filter for success',
    color: 'red',
    ui: {
      ...appConfig.ui.toast.slots,
      wrapper: 'fixed bottom-4 left-4 w-full max-w-sm', // Set max width to a larger value (max-w-sm or max-w-md)
      container: 'bg-[color:var(--primary-50)] text-white rounded-lg shadow-lg',
    }
  })
}

const sizeItems = generateFilterItems(SATELLITE_SIZE, selectedSize);
const typeItems = generateFilterItems(SATELLITE_TYPE, selectedType);
const orbitItems = generateFilterItems(ORBITAL_LEVELS, selectedOrbit);
const categoryItems = generateFilterItems(DISCIPLINES, selectedCategory);
const countryItems = generateFilterItems(COUNTRIES, selectedCountry);



const welcomeToSpysat = () => {
  const panelDetails = document.querySelector('.panel-details');

  panelDetails.innerHTML = "<div class=\"header-container\"><h2 class=\"title1 center\">Welcome to</h2><h1 class=\"title2\">SpySat</h1><br>"
  panelDetails.innerHTML += "<p>This website offers an interactive satellite visualization tool, featuring live tracking data and a wealth of information to explore. Around the globe, you can view <strong class=\"green\">satellites</strong>, <strong class=\"red\">debris</strong>, <strong class=\"orange\">rocket bodies</strong>, and <strong class=\"yellow\">unknown</strong> objects orbiting Earth.</p>"
  panelDetails.innerHTML += "<br>"
  panelDetails.innerHTML += "<h3>Use these controls to explore our globe</h3>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">Select</strong>: Left-click</p>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">Zoom</strong>: Scroll up or down </p>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">Rotate</strong>: Left-click and drag</p>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">Enter</strong>: Close the side panel for an enhanced view</p>"
  panelDetails.innerHTML += "<br>"
  panelDetails.innerHTML += "<h3>Use filters to find satellites that match your preferences and narrow the display</h3>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">SIZE</strong>: Filter satellites based on their Radar Cross Section (RCS), or size</p>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">TYPE</strong>: Filter by the satellite's classification, such as satellite, debris, rocket body, or unknown </p>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">ORBIT</strong>: Filter satellites by their orbital level</p>"
  panelDetails.innerHTML += "<p style=\"padding-left: 3em;\" class=\"sidenote\"> *SpySat supports LEO, MEO, HEO, and GEO orbits; all others are grouped as OTHER</p>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">CATEGORY</strong>: Filter satellites based on their function in orbit</p>"
  panelDetails.innerHTML += "<p style=\"padding-left: 3em;\" class=\"sidenote\">* This filter applies only to regular satellites with a payload</p>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">COUNTRY</strong>: Filter satellites by their country of origin</p>"
  panelDetails.innerHTML += "<h4>(to unselect a filter, select the same option again)</h4>"
  panelDetails.innerHTML += "<br>"
  panelDetails.innerHTML += "<h3>Use the search bar to quickly locate satellites by name or search for a specific satellite using its unique COSPAR ID</h3>"
  panelDetails.innerHTML += "<p style=\"padding-left: 2em;\"><strong class=\"emphasis blue\">Example</strong>: Type 'ISS' to find satellites related to the International Space Station, or enter '1990-037B' to search for the Hubble Space Telescope (HST) by its Object ID</p>"
  panelDetails.innerHTML += "<br>"
}

const satellitePanelDisplay = (satellite) => {
  const closeBar = document.querySelector(".close-bar")
  switch (satellite.OBJECT_TYPE) {
    case "PAYLOAD":
      closeBar.style.backgroundColor = 'var(--highlight-satelliteGreen)'
      break;
    case "ROCKET BODY":
      closeBar.style.backgroundColor = 'var(--highlight-rocketOrange)'
      break;
    case "DEBRIS":
      closeBar.style.backgroundColor = 'var(--highlight-debrisRed)'
      break;
    case "UNKNOWN":
      closeBar.style.backgroundColor = 'var(--highlight-unknownYellow)'
      break;
    default:
      closeBar.style.backgroundColor = 'var(--interaction)'
      break;
  }
  const panelDetails = document.querySelector('.panel-details');

  let image = satellite.IMAGE != "No image available" ? satellite.IMAGE : satellite.OBJECT_TYPE == "ROCKET BODY" ? "Rocket_Part.png" : satellite.OBJECT_TYPE == "DEBRIS" ? "Debris.jpg" : "Satellite.jpg";

  panelDetails.innerHTML = `<img src="${image}"  class="satelliteImages">`
  if (image == "Rocket_Part.png" || image == "Debris.jpg" || image == "Satellite.jpg") {
    panelDetails.innerHTML += "<p class=\"sidenote center\">Visual representation to depict the object appearance</p>"
  }
  panelDetails.innerHTML += "<br>"
  panelDetails.innerHTML += `<h2 class=\"blue\">${satellite.OBJECT_NAME}</h2>`
  panelDetails.innerHTML += `<h3 class=\"center\">${satellite.OBJECT_ID}</h2>`
  switch (satellite.OBJECT_TYPE) {
    case "PAYLOAD":
      panelDetails.innerHTML += `<h3 class=\"center green\">${satellite.OBJECT_TYPE}</h2>`
      break;
    case "ROCKET BODY":
      panelDetails.innerHTML += `<h3 class=\"center orange\">${satellite.OBJECT_TYPE}</h2>`
      break;
    case "DEBRIS":
      panelDetails.innerHTML += `<h3 class=\"center red\">${satellite.OBJECT_TYPE}</h2>`
      break;
    default:
      panelDetails.innerHTML += `<h3 class=\"center yellow\">${satellite.OBJECT_TYPE}</h2>`
  }
  panelDetails.innerHTML += `<br>`
  panelDetails.innerHTML += `<p><strong class=\"emphasis blue\">NORAD Catalog ID</strong>: ${satellite.NORAD_CAT_ID}</p>`
  let country = satellite.COUNTRY_CODE
  for (var i = 0; i < COUNTRY_CODES.length - 1; i++) {
    if (COUNTRY_CODES[i] == satellite.COUNTRY_CODE) {
      country = COUNTRIES[i]
    }
  }
  panelDetails.innerHTML += `<p><strong class=\"emphasis blue\">Country of Origin</strong>: ${toProperCase(country)}</p>`
  let launchDate = satellite.LAUNCH_DATE
  const formattedDate = new Date(launchDate).toLocaleDateString("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
  panelDetails.innerHTML += `<p><strong class=\"emphasis blue\">Launch Date</strong>: ${toProperCase(formattedDate)}</p>`
  panelDetails.innerHTML += `<p><strong class=\"emphasis blue\">Launch Site</strong>: ${satellite.SITE}</p>`
  panelDetails.innerHTML += `<p><strong class=\"emphasis blue\">RCS Size</strong>: ${satellite.RCS_SIZE}</p>`
  panelDetails.innerHTML += `<p><strong class=\"emphasis blue\">Level of Oribit</strong>: ${satellite.SATELLITE_ORBIT}</p>`
  panelDetails.innerHTML += `<p><strong class=\"emphasis blue\">Revolutions since Launch</strong>: ${satellite.REV_AT_EPOCH}</p>`
  if (satellite.OBJECT_TYPE == "PAYLOAD") {
    if (satellite.DISCIPLINES.length != 0) {
      panelDetails.innerHTML += `<p><strong class=\"emphasis blue\">Satellite Categories</strong>:</p>`
      for (let discipline of satellite.DISCIPLINES) {
        panelDetails.innerHTML += `<p style=\"padding-left: 2em;\">${discipline}</p>`
      }
    }
  }
  panelDetails.innerHTML += `<br>`
  panelDetails.innerHTML += `<h3>${satellite.DESCRIPTION}</h3>`

  panelDetails.innerHTML += `<br>`
  panelDetails.innerHTML += `<p><strong class=\"emphasis blue\">TLE</strong>:</p>`
  panelDetails.innerHTML += `<p style=\"padding-left: 2em;\" class=\"sidenote\">${satellite.TLE_LINE0}</p>`
  panelDetails.innerHTML += `<p style=\"padding-left: 2em;\" class=\"sidenote\">${satellite.TLE_LINE1}</p>`
  panelDetails.innerHTML += `<p style=\"padding-left: 2em;\" class=\"sidenote\">${satellite.TLE_LINE2}</p>`

  if (satellite.OBJECT_TYPE == "PAYLOAD") {
    getApproximatedSatelliteImage(satellite)
  }
}

const getApproximatedSatelliteImage = async (satellite) => {
  try {
    const response = await axios.get(`http://localhost:8080/images/get?NORAD_CAT_ID=${satellite.NORAD_CAT_ID}`);
    console.log(response)
    const panelDetails = document.querySelector('.panel-details');
    if (response.message != "Not Found") {
      panelDetails.innerHTML += `<br>`
      panelDetails.innerHTML += `<p class=\"sidenote center blue\">Satellite approximated view of Earth based on latitude and longitude  </p>`
      panelDetails.innerHTML += `<img src="${response.data.url}"  class="satelliteImages">`
    } else {
      console.log("image not found")
    }
  } catch (error) {
    console.error("Error fetching satellites:", error);
  }
}

function toProperCase(str) {
  return str
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}


</script>


<style>

/* .custom-toast-container {
  font-size: 200px !important;
  background-color: var(--primary-50);
  color: black;
  border-radius: 0.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
} */


.app-container {
  display: flex;
  flex-direction: column;
  height: 1vw;
  overflow: hidden;
}

.main-content {
  display: flex;
  flex-grow: 1;
  position: relative;
  height: calc(100vh - 4.5vw);
  transition: margin-right 0.75s ease-in-out;
}

#canvas-container {
  height: 100%;
  transition: width 0.75s ease-in-out;
}

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
  width: 1rem;
  height: 100%;
  /* background-color: var(--interaction); */
  cursor: pointer;
  position: sticky;
  top: 0;
}

.panel-details {
  padding: 1rem;
  flex: 1;
  color: var(--primary-500);
}

.logo {
  width: 70px;
  height: auto;
  max-width: 100%;
  margin-left: -5px;
}

.nav-items>* {
  margin-right: 10px;
}

.large-dropdown {
  width: 220px;
}

.large-button {
  width: 180px;
  height: 20px;
  font-size: 1.2m;
  /* Adjust the font size */
  padding: 12px;
  /* Adjust padding */
}
/* 
.dropdown-menu {
  width: 400px;
  max-height: 0.5vh;
  overflow-y: auto;
}

.custom-dropdown .u-dropdown-items {
  max-height: 50vh;
  overflow-y: auto; 
  width: 20rem; 
} */


/* .dropdownItem {
  padding: 12px 20px;
  font-size: 22px;

} */

.indent {
  padding-left: 20px;
  /* Or margin-left */
}

.panel-content {
  height: 85vh;
  max-height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  display: flex;
  flex-direction: row;
}

.panel-details {
  flex-grow: 1;
  /* Take up remaining space */
  overflow-y: auto;
  /* Allow scrolling for content */
  padding: 1rem;
  /* Optional padding */
}

::-webkit-scrollbar {
  width: 10px;
}

::-webkit-scrollbar-track {
  border-radius: 10px;
  /* Rounded corners */
}

::-webkit-scrollbar-thumb {
  background: var(--interaction);
  /* Scrollbar thumb color */
  border-radius: 10px;
  /* Rounded corners */
  border: 3px solid var(--primary-50);
  /* Adds padding effect */
  min-height: 30px;
  max-height: 30px;
}

button {
  font-size: 18px;
  height: 50px;
}

.title2 {
  margin-top: -40px;
  margin-bottom: -20px;
  font-family: "Gemunu Libre", sans-serif;
  font-weight: 400;
  font-style: normal;
  font-size: 110px;
  text-align: center;
  color: var(--interaction);
}

.title1 {
  margin-top: 20px;
  font-family: "Gemunu Libre", sans-serif;
  font-weight: 400;
  font-style: normal;
  font-size: 55px;
}

h2 {
  margin-top: -10px;
  font-family: "Gemunu Libre", sans-serif;
  font-weight: 400;
  font-style: normal;
  font-size: 35px;
  text-align: center;
  color: var(--interaction);
}

p {
  padding-left: 8px;
  font-size: 20px;
}

h3 {
  padding-left: 8px;
  font-size: 20px;
  font-family: "Gemunu Libre", sans-serif;
  font-weight: 700;
  font-style: normal;
  color: var(--primary-400);
}

h4 {
  padding-left: 8px;
  font-size: 18px;
  font-family: "Gemunu Libre", sans-serif;
  font-weight: 700;
  font-style: normal;
  color: var(--primary-300)
}

.center {
  text-align: center;
}

.emphasis {

  font-size: 22px;
  font-family: "Gemunu Libre", sans-serif;
  font-weight: 800;
  font-style: normal;
}

.sidenote {
  font-size: 17px;
  color: var(--primary-300)
}

.blue {
  color: var(--interaction);
}

.green {
  color: var(--highlight-satelliteGreen)
}

.yellow {
  color: var(--highlight-unknownYellow)
}

.red {
  color: var(--highlight-debrisRed)
}

.orange {
  color: var(--highlight-rocketOrange)
}

.header-container {
  position: relative;
  /* Keeps text above the pseudo-element */
  text-align: center;
  /* Centers the text */
  padding: 20px;
  height: 300px;
}

.header-container::before {
  content: "";
  /* Creates the pseudo-element */
  position: absolute;
  /* Positions it behind the text */
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url('spysat_logo.png');
  /* Path to your image */
  background-size: cover;
  /* Makes the image cover the entire container */
  background-position: center;
  /* Centers the image */
  background-repeat: no-repeat;
  /* Ensures the image doesn't repeat */
  opacity: 0.25;
  /* Adjust transparency of the image */
  z-index: -1;
  /* Places the image behind the text */
}

h1,
h2,
p {
  position: relative;
  z-index: 1;
  color: white;
}

.custom-dropdown {
  max-width: 100px;
  height: 50px;
}

.custom-button {
  width: 100%;
  height: 100%;
  font-size: 20px;

  font-family: "Gemunu Libre", sans-serif;
  font-weight: 400;
  font-style: normal;

  text-align: center;
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;

  background-color: #062c43;
  color: #ffffff;
}

.custom-button:hover {
  background-color: #054569;
  color: var(--interaction);
}

#satellitePopup {
  user-select: none;
  /* Prevent text selection */
  pointer-events: none;
  /* Prevent mouse interaction */
  position: absolute;
  background: #fff;
  /* Adjust background if needed */
  padding: 10px;
  border-radius: 5px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  z-index: 1000;
}

.satelliteImages {
  align-self: center;
  width: 400px
}
</style>
