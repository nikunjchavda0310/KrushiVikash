function loadVerificationDetails(farmerId) {
    // 1. First, check if the function is even being called
    console.log("Function called for Farmer ID:", farmerId);

    const panel = document.getElementById('reviewPanel');
    const content = document.getElementById('verificationContent');

    if (!panel || !content) {
        alert("Error: HTML elements 'reviewPanel' or 'verificationContent' are missing!");
        return;
    }

    // 2. Try to fetch the data
    fetch('/admin/get-verification/' + farmerId)
    .then(res => {
        if (!res.ok) {
            throw new Error("HTTP error! Status: " + res.status);
        }
        return res.json();
    })
    .then(data => {
        console.log("Data received:", data);

        if (!data || !data.id) {
            alert("No verification details found for this farmer.");
            return;
        }

        panel.style.display = 'block';
        content.innerHTML = `
            <tr class="text-center">
                <td class="fw-bold">${data.farmerName}</td>
                <td>${data.state}</td>
                <td>${data.district}</td>
                <td>${data.taluka}</td>
                <td>${data.village}</td>
                <td>${data.pincode}</td>
                <td class="text-success fw-bold">${data.regNumber}</td>
                <td>${data.farmArea}</td>
                <td>
                    <div class="d-flex flex-column gap-1">
                        <a href="/uploads/documents/${data.satBaraFile}" target="_blank" class="btn btn-sm btn-outline-dark">
                            <i class="fas fa-eye"></i> 7/12
                        </a>
                        <a href="/uploads/documents/${data.aadhaarFile}" target="_blank" class="btn btn-sm btn-outline-dark">
                            <i class="fas fa-id-card"></i> Aadhaar
                        </a>
                    </div>
                </td>
                <td>
                    ${data.status === 'PENDING' ?
                        `<button class="btn btn-success btn-sm w-100" onclick="approveFarmer(${data.id})">
                            <i class="fas fa-check"></i> Approve
                         </button>` :
                        `<span class="badge bg-success">VERIFIED</span>`
                    }
                </td>
            </tr>
        `;
        panel.scrollIntoView({ behavior: 'smooth' });
    })
    .catch(err => {
        console.error("Fetch Error:", err);
        alert("Could not load details. Check the Console (F12) for errors.");
    });
}