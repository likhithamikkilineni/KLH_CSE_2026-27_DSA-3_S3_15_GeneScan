const API_BASE_URL = "http://localhost:8080/api";

/**
 * Check whether the Java backend is online.
 */
export async function checkServerHealth() {
  const response = await fetch(`${API_BASE_URL}/health`);

  if (!response.ok) {
    throw new Error("GeneScan Java server is not available.");
  }

  return response.json();
}


/**
 * Send DNA sequence and motif to the Java
 * Boyer–Moore analysis engine.
 */
export async function analyzeDNA(sequence, motif) {
  const response = await fetch(`${API_BASE_URL}/analyze`, {
    method: "POST",

    headers: {
      "Content-Type": "application/json",
    },

    body: JSON.stringify({
      sequence,
      motif,
    }),
  });


  let data;

  try {
    data = await response.json();
  } catch {
    throw new Error(
      "The Java server returned an invalid response."
    );
  }


  if (!response.ok) {
    throw new Error(
      data.error || "DNA analysis failed."
    );
  }


  return data;
}