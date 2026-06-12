import { useState, useRef } from "react";
import axios from "axios";
import cytoscape from "cytoscape";
import type { Core } from "cytoscape";
import {
  Upload,
  Network,
  ExternalLink,
  AlertCircle,
  CheckCircle,
} from "lucide-react";

const API_BASE = "http://localhost:8080";

interface GraphDataItem {
  sourceId: number | string;
  sourceName?: string;
  targetId: number | string;
  targetName?: string;
  relationship: string;
}

interface QueryResult {
  [key: string]: unknown;
}

function App() {
  const [activeTab, setActiveTab] = useState<"ingest" | "query" | "graph">(
    "ingest",
  );

  // Ingest state
  const [apiName, setApiName] = useState("");
  const [apiUrl, setApiUrl] = useState(
    "https://petstore.swagger.io/v2/swagger.json",
  );
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [ingestMessage, setIngestMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const [ingestLoading, setIngestLoading] = useState(false);

  // Query state
  const [queryText, setQueryText] = useState("");
  const [queryResults, setQueryResults] = useState<QueryResult[]>([]);
  const [queryAnswer, setQueryAnswer] = useState<string | null>(null);
  const [queryLoading, setQueryLoading] = useState(false);
  const [queryError, setQueryError] = useState<string | null>(null);

  // Graph state
  const graphContainerRef = useRef<HTMLDivElement>(null);
  const cyInstanceRef = useRef<Core | null>(null);
  const [graphLoading, setGraphLoading] = useState(false);
  const [graphError, setGraphError] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    setSelectedFile(file);
  };

  // ==================== INGEST ====================
  const handleIngest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!apiName.trim()) {
      setIngestMessage({ type: "error", text: "API Name is required" });
      return;
    }

    setIngestLoading(true);
    setIngestMessage(null);

    const formData = new FormData();
    formData.append("apiName", apiName.trim());
    if (apiUrl.trim()) formData.append("url", apiUrl.trim());
    if (selectedFile) formData.append("file", selectedFile);

    try {
      const response = await axios.post(`${API_BASE}/api/ingest`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setIngestMessage({
        type: "success",
        text: response.data || "API successfully ingested!",
      });
      setApiName("");
      setSelectedFile(null);
    } catch (err: unknown) {
      const msg = axios.isAxiosError(err)
        ? err.response?.data || err.message
        : "Ingestion failed";
      setIngestMessage({ type: "error", text: String(msg) });
    } finally {
      setIngestLoading(false);
    }
  };

  // ==================== QUERY ====================
  const handleQuery = async () => {
    if (!queryText.trim()) return;
    setQueryLoading(true);
    setQueryError(null);
    setQueryResults([]);
    setQueryAnswer(null);

    try {
      const res = await axios.post(`${API_BASE}/api/discover/semantic`, {
        query: queryText.trim(),
      });
      const data = res.data || {};
      setQueryAnswer(
        data && typeof data === "object" && "naturalLanguageAnswer" in data
          ? (data.naturalLanguageAnswer as string) || null
          : null,
      );
      const results = Array.isArray(data.results)
        ? data.results
        : data.results
          ? [data.results]
          : Array.isArray(data)
            ? data
            : data
              ? [data]
              : [];
      setQueryResults(results);
    } catch (err: unknown) {
      setQueryError(axios.isAxiosError(err) ? err.message : "Query failed");
    } finally {
      setQueryLoading(false);
    }
  };

  // ==================== GRAPH ====================
  const loadGraph = async () => {
    setGraphLoading(true);
    setGraphError(null);

    try {
      const res = await axios.get<GraphDataItem[]>(`${API_BASE}/api/graph/data`);
      const data = res.data;

      if (!data?.length) {
        setGraphError("No data found. Please ingest some APIs first.");
        return;
      }

      const nodeMap = new Map();
      const edges: { source: string; target: string; label: string }[] = [];

      data.forEach((item) => {
        const sId = String(item.sourceId);
        const tId = String(item.targetId);

        if (!nodeMap.has(sId))
          nodeMap.set(sId, {
            id: sId,
            label: item.sourceName || `API ${sId}`,
            type: "ApiService",
          });
        if (!nodeMap.has(tId))
          nodeMap.set(tId, {
            id: tId,
            label: item.targetName || `Endpoint ${tId}`,
            type: "Endpoint",
          });

        edges.push({ source: sId, target: tId, label: item.relationship });
      });

      const nodes = Array.from(nodeMap.values()).map((n) => ({ data: n }));
      const cytoEdges = edges.map((e, i) => ({ data: { id: `e${i}`, ...e } }));

      if (cyInstanceRef.current) cyInstanceRef.current.destroy();

      const cy = cytoscape({
        container: graphContainerRef.current!,
        elements: [...nodes, ...cytoEdges],
        style: [
          {
            selector: 'node[type="ApiService"]',
            style: {
              "background-color": "#3b82f6",
              label: "data(label)",
              color: "#fff",
              "font-size": "13px",
              width: "110px",
              height: "46px",
              shape: "round-rectangle",
            },
          },
          {
            selector: 'node[type="Endpoint"]',
            style: {
              "background-color": "#10b981",
              label: "data(label)",
              color: "#fff",
              "font-size": "11px",
              width: "85px",
              height: "34px",
              shape: "round-rectangle",
            },
          },
          {
            selector: "edge",
            style: {
              width: 2,
              "line-color": "#64748b",
              "target-arrow-color": "#64748b",
              "target-arrow-shape": "triangle",
              "curve-style": "bezier",
            },
          },
        ],
        layout: { name: "cose", animate: true, padding: 50 },
      });

      cyInstanceRef.current = cy;
      cy.ready(() => cy.fit(undefined, 40));
    } catch {
      setGraphError("Failed to load graph");
    } finally {
      setGraphLoading(false);
    }
  };

  // Note: Graph loads explicitly via the "Load / Refresh Graph" button to avoid
  // effect-driven setState and to give the user control. The container renders
  // immediately when the tab is selected.

  return (
    <div className="min-h-screen bg-[#0a0f1a] text-[#e0e7ff]">
      {/* Header */}
      <header className="border-b border-slate-800 bg-[#0a0f1a]/90 backdrop-blur-lg sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-8 h-20 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-blue-600 rounded-2xl flex items-center justify-center">
              <Network className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                API Discovery
              </h1>
              <p className="text-xs text-slate-500 -mt-1">
                Enterprise Knowledge Graph
              </p>
            </div>
          </div>
          <a
            href="http://localhost:7474"
            target="_blank"
            className="flex items-center gap-2 text-sm text-blue-400 hover:text-blue-300"
          >
            Open Neo4j Browser <ExternalLink className="w-4 h-4" />
          </a>
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-8 pt-10 pb-16">
        {/* Tabs */}
        <div className="flex justify-center mb-10">
          <div className="inline-flex bg-slate-900 p-1.5 rounded-3xl border border-slate-800">
            <button
              onClick={() => setActiveTab("ingest")}
              className={`tab ${activeTab === "ingest" ? "tab-active" : "tab-inactive"}`}
            >
              Ingest OpenAPI
            </button>
            <button
              onClick={() => setActiveTab("query")}
              className={`tab ${activeTab === "query" ? "tab-active" : "tab-inactive"}`}
            >
              Natural Language Query
            </button>
            <button
              onClick={() => setActiveTab("graph")}
              className={`tab ${activeTab === "graph" ? "tab-active" : "tab-inactive"}`}
            >
              Property Graph
            </button>
          </div>
        </div>

        {/* INGEST TAB */}
        {activeTab === "ingest" && (
          <div className="max-w-2xl mx-auto">
            <div className="card">
              <div className="mb-8">
                <h2 className="text-2xl font-semibold tracking-tight mb-2">
                  Ingest OpenAPI
                </h2>
                <p className="text-slate-400">
                  Add a new API to the Knowledge Graph.
                </p>
              </div>

              <form onSubmit={handleIngest} className="space-y-6">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    API Name
                  </label>
                  <input
                    type="text"
                    value={apiName}
                    onChange={(e) => setApiName(e.target.value)}
                    placeholder="e.g. PaymentService"
                    className="input"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    OpenAPI URL (optional)
                  </label>
                  <input
                    type="url"
                    value={apiUrl}
                    onChange={(e) => setApiUrl(e.target.value)}
                    placeholder="https://petstore.swagger.io/v2/swagger.json"
                    className="input"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    Upload OpenAPI File
                  </label>
                  <label className="upload-zone flex flex-col items-center justify-center rounded-3xl p-9 cursor-pointer text-center">
                    <Upload className="w-8 h-8 text-slate-400 mb-3" />
                    <span className="font-medium">
                      Click or drag file to upload
                    </span>
                    <span className="text-xs text-slate-500 mt-1">
                      JSON or YAML supported
                    </span>
                    <input
                      type="file"
                      accept=".json,.yaml,.yml"
                      onChange={handleFileChange}
                      className="hidden"
                    />
                  </label>
                  {selectedFile && (
                    <p className="mt-2 text-sm text-emerald-400 flex items-center gap-2">
                      <CheckCircle className="w-4 h-4" />
                      {selectedFile.name}
                    </p>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={ingestLoading || !apiName.trim()}
                  className="btn btn-primary w-full h-12 text-base"
                >
                  {ingestLoading
                    ? "Ingesting..."
                    : "Ingest into Knowledge Graph"}
                </button>
              </form>

              {ingestMessage && (
                <div
                  className={`mt-6 p-4 rounded-2xl text-sm flex gap-3 ${ingestMessage.type === "success" ? "bg-emerald-950 border border-emerald-900 text-emerald-400" : "bg-red-950 border border-red-900 text-red-400"}`}
                >
                  {ingestMessage.type === "success" ? (
                    <CheckCircle className="w-5 h-5 mt-0.5" />
                  ) : (
                    <AlertCircle className="w-5 h-5 mt-0.5" />
                  )}
                  {ingestMessage.text}
                </div>
              )}
            </div>
          </div>
        )}

        {/* QUERY TAB */}
        {activeTab === "query" && (
          <div className="max-w-4xl mx-auto">
            <div className="card mb-8">
              <h2 className="text-2xl font-semibold mb-2">
                Natural Language Query
              </h2>
              <div className="flex gap-3 mt-4">
                <input
                  type="text"
                  value={queryText}
                  onChange={(e) => setQueryText(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleQuery()}
                  placeholder="Ask anything about your APIs..."
                  className="input flex-1"
                />
                <button
                  onClick={handleQuery}
                  disabled={queryLoading || !queryText.trim()}
                  className="btn btn-primary px-10"
                >
                  Search
                </button>
              </div>
            </div>

            <div className="card min-h-[420px]">
              {queryError && <div className="text-red-400 mb-4">{queryError}</div>}

              {queryAnswer && (
                <div className="mb-6 p-4 rounded-2xl bg-slate-800 border border-slate-700">
                  <div className="text-xs uppercase tracking-widest text-slate-400 mb-1">Answer</div>
                  <div className="text-sm leading-relaxed">{queryAnswer}</div>
                </div>
              )}

              {queryResults.length > 0 ? (
                <div className="space-y-3">
                  {queryResults.map((r, i) => {
                    const result = r as {
                      method?: string;
                      path?: string;
                      apiName?: string;
                      apiVersion?: string;
                      summary?: string;
                      description?: string;
                      parameters?: string[];
                    };
                    return (
                      <div key={i} className="result-item">
                        <div className="flex items-center gap-2 flex-wrap mb-2">
                          {result.method && (
                            <span className="inline-block text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-900/60 text-emerald-300">
                              {result.method}
                            </span>
                          )}
                          {result.path && (
                            <span className="font-mono text-sm text-emerald-400">{result.path}</span>
                          )}
                          {result.apiName && (
                            <span className="text-xs text-slate-400 ml-auto">
                              {result.apiName} {result.apiVersion ? `v${result.apiVersion}` : ""}
                            </span>
                          )}
                        </div>
                        {result.summary && (
                          <div className="text-sm font-medium text-slate-200 mb-1">{result.summary}</div>
                        )}
                        {result.description && (
                          <div className="text-xs text-slate-400 line-clamp-2">{result.description}</div>
                        )}
                        {result.parameters && Array.isArray(result.parameters) && result.parameters.length > 0 && (
                          <div className="mt-2 text-[11px] text-slate-500">
                            Params: {result.parameters.join(", ")}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              ) : !queryAnswer && !queryLoading && !queryError ? (
                <div className="text-center py-16 text-slate-500">
                  Your results will appear here
                </div>
              ) : null}
            </div>
          </div>
        )}

        {/* GRAPH TAB */}
        {activeTab === "graph" && (
          <div>
            <div className="flex justify-between items-center mb-6">
              <div>
                <h2 className="text-2xl font-semibold">Knowledge Graph</h2>
                <p className="text-sm text-slate-400">
                  ApiService → Endpoint relationships
                </p>
              </div>
              <button
                onClick={loadGraph}
                disabled={graphLoading}
                className="btn btn-secondary"
              >
                {graphLoading ? "Loading..." : "Load / Refresh Graph"}
              </button>
            </div>

            {graphLoading && (
              <div className="text-sm text-slate-400 mb-3">Loading graph visualization...</div>
            )}
            {graphError && (
              <div className="text-sm text-red-400 mb-3 p-3 bg-red-950/50 rounded-2xl border border-red-900">
                {graphError}
              </div>
            )}

            <div
              ref={graphContainerRef}
              className="w-full h-[620px] bg-slate-900 rounded-3xl border border-slate-800"
            />
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
