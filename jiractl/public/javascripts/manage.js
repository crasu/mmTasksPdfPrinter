var global = {
  addProject: function () {
    if(document.getElementById('addProjectId').value && document.getElementById('addPassword').value) {
      document.getElementById('addProject').action += document.getElementById('addProjectId').value + "/" + document.getElementById('addPassword').value;
      return true;
    } else {
      alert("Please Enter a ProjectId and a Password");
      return false;
    }
  }
};

document.getElementById('addProject').onsubmit = global.addProject;
