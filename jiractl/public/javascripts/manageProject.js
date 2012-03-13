var global = {
  addUser: function () {
    if(document.getElementById('addUserName').value) {
      document.getElementById('addUser').action += document.getElementById('addUserName').value;
      return true;
    } else {
      alert("Please specify the Username for the new User.");
      return false;
    }
  },
  delUser: function () {
    if(document.getElementById('delUserName').value) {
      document.getElementById('delUser').action += document.getElementById('delUserName').value;
      return true;
    } else {
      alert("Please specify the Username to delete the User.");
      return false;
    }
  },
  confirmDeleteProject: function () {
    return confirm("Do you really want to delete this Project?");
  }
};

document.getElementById('addUser').onsubmit = global.addUser;
document.getElementById('delUser').onsubmit = global.delUser;
document.getElementById('closeProject').onsubmit = global.confirmDeleteProject;
