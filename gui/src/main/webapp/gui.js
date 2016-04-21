/** @jsx React.DOM */

var eve = require('evejs');
var React = require('react');
var ReactDOM = require('react-dom');

//EVE section, setting up an agent with websocket client connection to backends.
eve.system.init({
  transports: [
    {
      type: 'ws',
      url: 'ws://localhost:3000/agents/:id'
    },
    {
      type: 'http',
      port: 3000,
      url: 'http://localhost:3000/agents/:id'
    }
  ]
})

function MonitorAgent(id) {
  // execute super constructor
  eve.Agent.call(this, id);
  this.rpc = this.loadModule('rpc', this.rpcFunctions, {timeout: 2000}); // option 1

}

MonitorAgent.prototype = Object.create(eve.Agent.prototype);
MonitorAgent.prototype.constructor = MonitorAgent;

MonitorAgent.prototype.rpcFunctions = {};
MonitorAgent.prototype.rpcFunctions.updatePosition = function (params) {
  var id = params.id;
  if (id != undefined) {
    if (typeof viewer != "undefined") {
      var newState = {};
      newState[id] = params;
      viewer.setState(newState);

      if (typeof controller != "undefined") {
        controller.setState(function (prev, props) {
          if (prev.id == id && (prev.speed != newState[id].speed || prev.heading != newState[id].heading)) {
            return {"speed": newState[id].speed, "heading": newState[id].heading};
          } else {
            return {};
          }
        });
      }
    } else {
      console.log("viewer is undefined");
    }
  } else {
    console.warn("Incorrect vehicle data received, id missing");
  }
}

MonitorAgent.prototype.reconnect = function () {
  this.disconnect(eve.system.transports.getAll());
  this.connect(eve.system.transports.getAll());
}

MonitorAgent.prototype.subscribe = function (manager) {
  //Open WS to manager
  this.send("ws://localhost:8081/agents/manager", {"jsonrpc": "2.0", "id": "1", "method": "registerUI"});
}

MonitorAgent.prototype.createVehicle = function () {
  this.send("ws://localhost:8081/agents/manager", {"jsonrpc": "2.0", "id": "1", "method": "createVehicle"});
}

MonitorAgent.prototype.setSpeed = function (vehicle, speed) {
  this.send("ws://localhost:8081/agents/manager", {
    "jsonrpc": "2.0",
    "id": "1",
    "method": "control",
    "params": {"id": vehicle, "method": "setSpeed", "params": {"speed": speed}}
  });
}

MonitorAgent.prototype.setHeading = function (vehicle, heading) {
  this.send("ws://localhost:8081/agents/manager", {
    "jsonrpc": "2.0",
    "id": "1",
    "method": "control",
    "params": {"id": vehicle, "method": "setHeading", "params": {"heading": heading}}
  });
}

//Make global on purpose for Console interaction:
agent = new MonitorAgent('monitor_' + Math.random());

agent.reconnect();
agent.subscribe();

//REACT section, updateing view
var vehicleRender = function (me, vehicle) {
  var boundClick = me.handleVehicle.bind(me, vehicle.id);
  return (<div onClick={boundClick} key={vehicle.id} className="vehicle"
               style={{bottom:(vehicle.x-5)+"px",left:(vehicle.y-5)+"px"}}></div> );
}

var vehiclesRender = function (me) {
  var rows = [];
  Object.keys(me.state).map(function (key) {
    rows.push(vehicleRender(me, me.state[key]));
  });
  return rows;
}

var Viewer = React.createClass({
  getInitialState: function () {
    return {};
  },
  handleVehicle: function (key, e) {
    controller.setState({"id": key, "speed": this.state[key].speed, "heading": this.state[key].heading});
  },
  render: function () {
    return (
      <div className="viewer">
        { vehiclesRender(this) }
      </div>
    );
  }
});

var viewer = ReactDOM.render(
  <Viewer />,
  document.getElementById('container')
);

function isNumeric(n) {
  return !isNaN(parseFloat(n)) && isFinite(n);
}

var Controller = React.createClass({
  getInitialState: function () {
    return {};
  },
  handlespeedchange: function (e) {
    if (typeof e.target.value != "undefined" && isNumeric(e.target.value)) {
      agent.setSpeed(this.state.id, e.target.value);
    }
    this.setState({"speed": e.target.value});
  },
  handleheadingchange: function (e) {
    if (typeof e.target.value != "undefined" && isNumeric(e.target.value)) {
      agent.setHeading(this.state.id, e.target.value);
    }
    this.setState({"heading": e.target.value});
  },
  render: function () {
    if (typeof this.state.speed == "undefined") {
      return (
        <div className="controller"/>
      )
    } else {
      return (
        <div className="controller">
          Speed: &nbsp;&nbsp;&nbsp;&nbsp;<input className="manualInput" value={ this.state.speed }
                                                onChange={this.handlespeedchange}/> m/s <br/>
          Heading: <input className="manualInput" value={ this.state.heading } onChange={this.handleheadingchange}/>
          rad. <br/>
        </div>
      );
    }
  }
});

var controller = ReactDOM.render(
  <Controller />,
  document.getElementById('controller')
)
