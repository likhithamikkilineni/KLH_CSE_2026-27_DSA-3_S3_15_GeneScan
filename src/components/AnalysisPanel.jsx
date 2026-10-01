import { useState } from "react";
import { analyzeDNA } from "../api";
import "./AnalysisPanel.css";

function AnalysisPanel() {

  const [sequence, setSequence] = useState(
    "ATGCGTACGTAGCTAGCTAGCTAGCTAGCTA"
  );

  const [motif, setMotif] = useState(
    "GCTAGCTA"
  );

  const [result, setResult] = useState(null);

  const [loading, setLoading] = useState(false);

  const [error, setError] = useState("");

  const handleAnalyze = async () => {

    setError("");
    setResult(null);

    if (!sequence.trim()) {
      setError("Please enter a DNA sequence.");
      return;
    }

    if (!motif.trim()) {
      setError("Please enter a DNA motif.");
      return;
    }

    try {

      setLoading(true);

      const data = await analyzeDNA(
        sequence,
        motif
      );

      setResult(data);

    } catch (err) {

      setError(
        err.message ||
        "Unable to connect to the GeneScan server."
      );

    } finally {

      setLoading(false);

    }
  };


  return (
    <section className="analysis-panel">

      {/* =====================================================
          TITLE
      ===================================================== */}

      <div className="analysis-panel-heading">

        <span>
          BOYER–MOORE ANALYSIS ENGINE
        </span>

        <h2>
          Search Your DNA Sequence
        </h2>

        <p>
          Enter a DNA sequence and motif. GeneScan will send
          them to the Java Boyer–Moore engine for analysis.
        </p>

      </div>


      {/* =====================================================
          INPUT AREA
      ===================================================== */}

      <div className="analysis-input-grid">

        {/* DNA SEQUENCE */}

        <div className="input-card">

          <div className="input-card-header">

            <label>
              DNA SEQUENCE
            </label>

            <span>
              A / C / G / T
            </span>

          </div>


          <textarea
            value={sequence}
            onChange={(event) =>
              setSequence(event.target.value)
            }
            placeholder="Enter or paste DNA sequence..."
            spellCheck="false"
          />


          <div className="input-footer">

            <span>
              Length:
              {" "}
              {sequence.replace(/\s/g, "").length}
              {" "}
              bases
            </span>

          </div>

        </div>


        {/* MOTIF */}

        <div className="input-card motif-card">

          <div className="input-card-header">

            <label>
              DNA MOTIF
            </label>

            <span>
              Pattern to find
            </span>

          </div>


          <input
            type="text"
            value={motif}
            onChange={(event) =>
              setMotif(event.target.value.toUpperCase())
            }
            placeholder="Example: GCTAGCTA"
            spellCheck="false"
          />


          <div className="motif-preview">

            {motif
              ? motif
                  .split("")
                  .map((character, index) => (
                    <span key={index}>
                      {character}
                    </span>
                  ))
              : (
                <span className="empty-motif">
                  Enter a motif
                </span>
              )}

          </div>

        </div>

      </div>


      {/* =====================================================
          ERROR
      ===================================================== */}

      {error && (

        <div className="analysis-error">
          <span>!</span>
          {error}
        </div>

      )}


      {/* =====================================================
          ANALYZE BUTTON
      ===================================================== */}

      <button
        className="analyze-button"
        onClick={handleAnalyze}
        disabled={loading}
      >

        {loading ? (
          <>
            <span className="button-spinner"></span>
            Running Boyer–Moore...
          </>
        ) : (
          <>
            ⚡ Run Boyer–Moore Analysis
          </>
        )}

      </button>


      {/* =====================================================
          RESULTS
      ===================================================== */}

      {result && (

        <div className="results-container">

          <div className="results-heading">

            <div>
              <span>
                ANALYSIS COMPLETE
              </span>

              <h3>
                Boyer–Moore Search Results
              </h3>
            </div>

            <div className="success-badge">
              ✓ Completed
            </div>

          </div>


          {/* =================================================
              STATISTICS
          ================================================= */}

          <div className="stats-grid">

            <div className="stat-card">

              <span>
                SEQUENCE LENGTH
              </span>

              <strong>
                {result.sequenceLength}
              </strong>

              <small>
                bases
              </small>

            </div>


            <div className="stat-card">

              <span>
                MATCHES FOUND
              </span>

              <strong className="green-value">
                {result.matchCount}
              </strong>

              <small>
                occurrences
              </small>

            </div>


            <div className="stat-card">

              <span>
                COMPARISONS
              </span>

              <strong>
                {result.comparisons}
              </strong>

              <small>
                character checks
              </small>

            </div>


            <div className="stat-card">

              <span>
                SHIFTS
              </span>

              <strong>
                {result.shifts}
              </strong>

              <small>
                pattern movements
              </small>

            </div>


            <div className="stat-card">

              <span>
                EXECUTION TIME
              </span>

              <strong>
                {result.executionTimeMilliseconds}
              </strong>

              <small>
                milliseconds
              </small>

            </div>

          </div>


          {/* =================================================
              MATCH POSITIONS
          ================================================= */}

          <div className="result-section">

            <div className="result-section-heading">

              <span>
                MATCH POSITIONS
              </span>

              <small>
                0-indexed
              </small>

            </div>


            {result.matchPositions &&
             result.matchPositions.length > 0 ? (

              <div className="position-list">

                {result.matchPositions.map(
                  (position, index) => (

                    <div
                      className="position-chip"
                      key={`${position}-${index}`}
                    >
                      <span>
                        #{index + 1}
                      </span>

                      <strong>
                        {position}
                      </strong>
                    </div>

                  )
                )}

              </div>

            ) : (

              <div className="no-match">
                No occurrences of the motif were found.
              </div>

            )}

          </div>


          {/* =================================================
              BAD CHARACTER TABLE
          ================================================= */}

          <div className="result-section">

            <div className="result-section-heading">

              <span>
                BAD CHARACTER TABLE
              </span>

              <small>
                Last occurrence
              </small>

            </div>


            <div className="heuristic-grid">

              {["A", "C", "G", "T"].map(
                (character) => (

                  <div
                    className="heuristic-card"
                    key={character}
                  >

                    <strong>
                      {character}
                    </strong>

                    <span>
                      {result.badCharacterTable?.[character]}
                    </span>

                  </div>

                )
              )}

            </div>

          </div>


          {/* =================================================
              GOOD SUFFIX TABLE
          ================================================= */}

          <div className="result-section">

            <div className="result-section-heading">

              <span>
                GOOD SUFFIX TABLE
              </span>

              <small>
                Shift values
              </small>

            </div>


            <div className="suffix-table">

              {result.goodSuffixTable?.map(
                (shift, index) => (

                  <div
                    className="suffix-cell"
                    key={`${index}-${shift}`}
                  >

                    <span>
                      {index}
                    </span>

                    <strong>
                      {shift}
                    </strong>

                  </div>

                )
              )}

            </div>

          </div>


          {/* =================================================
              ALGORITHM INFO
          ================================================= */}

          <div className="algorithm-summary">

            <div className="summary-icon">
              ⚡
            </div>

            <div>

              <span>
                ALGORITHM USED
              </span>

              <strong>
                Boyer–Moore String Matching
              </strong>

              <p>
                Pattern comparisons are performed from
                right to left, with shifts calculated using
                the Bad Character and Good Suffix heuristics.
              </p>

            </div>

          </div>

        </div>

      )}

    </section>
  );
}

export default AnalysisPanel;