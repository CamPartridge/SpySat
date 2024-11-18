// plugins/three.js
import * as THREE from 'three';
export default ({ app }, inject) => {
  inject('THREE', THREE);
};
