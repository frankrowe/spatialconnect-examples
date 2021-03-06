'use strict';
/*global ol*/
var React = require('react');
var GPS = require('./gps');
var sc = require('spatialconnect');
var FeatureObs = require('./../stores/feature');
var ReactDOM = require('react-dom');

var MapView = React.createClass({
  getInitialState:function () {
    return { reloadDisabled : false}
  },
  componentDidMount: function() {
    var map = this.props.map;
    var me = this.refs.map;
    var elem = ReactDOM.findDOMNode(me);
    map.setTarget(elem);
    var that = this;
  },
  addFeature:function() {
    var map = this.props.map;
    var coord = map.getView().getCenter();
    var feature = new ol.Feature(new ol.geom.Point(coord));
    FeatureObs.create.onNext(feature);
  },
  geoSpatialQuery: function(map) {
    map.getLayers().getArray().map(function(layer) {
      if (layer.getSource() instanceof ol.source.Vector) {
        layer.getSource().clear();
      }
    });
    var extent = map.getView().calculateExtent(map.getSize());
    var f = sc.Filter().geoBBOXContains(extent);
    sc.action.geospatialQuery(f);
  },
  render: function() {
    return (
      <div className="container">
        <div>
          <div className="row">
            <div className="col-xs-4">
              <GPS>
              </GPS>
            </div>
            <div className="col-xs-3">
              <button onClick={this.addFeature}>
                Add Feature
              </button>
            </div>
            <div className="col-xs-3">
              <button onClick={this.geoSpatialQuery.bind(this,this.props.map)}>
                Reload Features
              </button>
            </div>
          </div>
        </div>
        <div className="row">
          <div ref="map" id="map">
          </div>
        </div>
      </div>
    );
  }
});

module.exports = MapView;
