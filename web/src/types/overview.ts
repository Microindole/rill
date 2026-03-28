export interface OverviewHighlight {
    label: string;
    value: string;
    detail: string;
}

export interface OverviewModule {
    name: string;
    role: string;
    releaseBoundary: string;
    details: string;
}

export interface OverviewCapability {
    category: string;
    title: string;
    details: string;
}

export interface OverviewExpansion {
    area: string;
    targetModule: string;
    approach: string;
    why: string;
}

export interface SystemOverview {
    appName: string;
    stage: string;
    positioning: string;
    highlights: OverviewHighlight[];
    modules: OverviewModule[];
    capabilities: OverviewCapability[];
    expansions: OverviewExpansion[];
}
