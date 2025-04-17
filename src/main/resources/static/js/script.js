function addEmployee() {
    const name = document.getElementById('employeeName').value;
    const id = document.getElementById('employeeId').value;
    fetch('http://localhost:8080/api/employees', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name: name, id: id })
    })
   .then(response => response.json())
   .then(data => {
        if (data.success) {
            alert('员工添加成功');
            document.getElementById('employeeName').value = '';
            document.getElementById('employeeId').value = '';
            loadEmployees();
        } else {
            alert('添加失败: ' + data.message);
        }
    })
   .catch(error => {
        alert('添加员工时出错: ' + error);
    });
}

function loadEmployees() {
    fetch('http://localhost:8080/api/employees')
   .then(response => response.json())
   .then(data => {
        const tableBody = document.querySelector('#employeeTable tbody');
        tableBody.innerHTML = '';
        data.forEach(employee => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${employee.id}</td>
                <td>${employee.name}</td>
                <td>
                    <button onclick="deleteEmployee(${employee.id})">删除</button>
                    <button onclick="editEmployee(${employee.id})">编辑</button>
                </td>
            `;
            tableBody.appendChild(row);
        });
    })
   .catch(error => {
        alert('加载员工信息时出错: ' + error);
    });
}

function deleteEmployee(id) {
    fetch(`http://localhost:8080/api/employees/${id}`, {
        method: 'DELETE'
    })
   .then(response => response.json())
   .then(data => {
        if (data.success) {
            alert('员工删除成功');
            loadEmployees();
        } else {
            alert('删除失败: ' + data.message);
        }
    })
   .catch(error => {
        alert('删除员工时出错: ' + error);
    });
}

function editEmployee(id) {
    const newName = prompt('请输入新的员工姓名');
    if (newName) {
        fetch(`http://localhost:8080/api/employees/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name: newName })
        })
       .then(response => response.json())
       .then(data => {
            if (data.success) {
                alert('员工信息更新成功');
                loadEmployees();
            } else {
                alert('更新失败: ' + data.message);
            }
        })
       .catch(error => {
            alert('更新员工信息时出错: ' + error);
        });
    }
}

// 页面加载时加载员工信息
window.onload = loadEmployees;
