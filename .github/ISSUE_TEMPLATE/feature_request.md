name: Feature Request
description: Suggest an idea or new feature for ArcX
title: "[FEATURE] "
labels: ["enhancement"]
body:
  - type: textarea
    id: problem
    attributes:
      label: Is your feature request related to a problem?
      description: A clear description of what the problem is (e.g., I'm always frustrated when...)
    validations:
      required: false
  - type: textarea
    id: solution
    attributes:
      label: Proposed Solution
      description: A clear and concise description of what you want to happen.
    validations:
      required: true
  - type: textarea
    id: alternatives
    attributes:
      label: Alternatives Considered
      description: A description of any alternative solutions or features you've considered.
    validations:
      required: false
